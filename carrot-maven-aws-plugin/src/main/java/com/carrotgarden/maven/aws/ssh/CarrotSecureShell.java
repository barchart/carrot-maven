/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.maven.aws.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.slf4j.Logger;

import com.carrotgarden.maven.aws.ssh.PathMaker.Entry;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;

/**
 * @author Andrei Pozolotin
 */
public class CarrotSecureShell {

	/** session connect retry, count */
	private final int connectRetries;

	/** session connect retry, milliseconds */
	private final long connectTimeout;

	private final Logger logger;

	private final File keyFile;

	private final String user;
	private final String host;
	private final int port;

	public CarrotSecureShell( //
			final Logger logger, //
			final File keyFile, //
			final String user, //
			final String host, //
			final int port, //
			final int retries, //
			final long timeout //
	) {

		this.logger = logger;

		this.keyFile = keyFile;

		this.user = user;
		this.host = host;
		this.port = port;

		this.connectRetries = retries;
		this.connectTimeout = timeout * 1000;

	}

	private Session newSession() throws Exception {

		logger.debug("exec getSession: on " + host + " : " + port + " user "
				+ user);

		final JSch jsch = new JSch();

		jsch.addIdentity(keyFile.getAbsolutePath());

		final Session session = jsch.getSession(user, host, port);

		session.setConfig("StrictHostKeyChecking", "no");

		sessionConnectWithRetry(session);

		return session;

	}

	/** assume sshd isnt running yet, wait and retry */
	private void sessionConnectWithRetry(final Session session)
			throws Exception {

		Exception cause = null;

		for (int count = 0; count < connectRetries; count++) {

			try {

				session.connect();

				logger.info("session connect success");

				return;

			} catch (final Exception e) {

				cause = e;

			}

			logger.info("session connect attempt : {}", count);

			Thread.sleep(connectTimeout);

		}

		if (cause == null) {
			throw new IllegalStateException("unexpected");
		} else {
			throw cause;
		}

	}

	public int execute(final String command) throws Exception {

		logger.info("exec user   : " + user);
		logger.info("exec host   : " + host);
		logger.info("exec port   : " + port);
		logger.info("exec command: " + command);

		final Session session = newSession();

		final ChannelExec channel = (ChannelExec) session.openChannel("exec");

		channel.setCommand(command);

		channel.setPty(true);

		channel.connect();

		//

		final InputStream input = channel.getInputStream();

		final Reader reader = new InputStreamReader(input);

		final BufferedReader buffered = new BufferedReader(reader);

		while (true) {

			final String line = buffered.readLine();

			if (line == null) {
				break;
			}

			logger.info(">>> " + line);

		}

		//

		input.close();

		//

		int count = 50;
		final int delay = 100;

		while (true) {
			if (channel.isClosed()) {
				break;
			}
			if (count-- < 0) {
				break;
			}
			Thread.sleep(delay);
		}

		logger.info("exec channel closed: " + channel.isClosed());

		final int status = channel.getExitStatus();

		logger.info("exec exit status: " + status);

		channel.disconnect();

		session.disconnect();

		return status;

	}

	private String makePath(final String root, final String base) {
		if ("/".equals(root)) {
			return "/" + base;
		} else {
			return root + "/" + base;
		}
	}

	private void ensureTargetFolder(final ChannelSftp channel,
			final String folder) throws Exception {

		logger.debug("sftp ensure: " + folder);

		final String[] pathArray = folder.split("/");

		String root = "/";

		for (final String path : pathArray) {

			if (path.length() == 0) {
				continue;
			}

			if (".".equals(path)) {
				continue;
			}

			/** absolute */
			channel.cd(root);

			/** absolute */
			final String next = makePath(root, path);

			boolean isPresent = false;

			try {
				channel.stat(next);
				isPresent = true;
			} catch (final Exception e) {
				isPresent = false;
			}

			if (isPresent) {
				logger.debug("sftp present: " + next);
			} else {
				/** relative to root */
				logger.debug("sftp creating: " + next + " path " + path);
				channel.mkdir(path);
				logger.debug("sftp created: " + next);
			}

			root = next;

		}

	}

	public int publish(final String source, final String target)
			throws Exception {

		logger.info("sftp user  : " + user);
		logger.info("sftp host  : " + host);
		logger.info("sftp port  : " + port);
		logger.info("sftp source: " + source);
		logger.info("sftp target: " + target);

		final Session session = newSession();

		final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");

		channel.connect();

		//

		final SftpProgressMonitor monitor = new SftpProgressMonitor() {

			@Override
			public void init(final int op, final String source,
					final String target, final long max) {
				logger.info("sftp upload: " + target);
			}

			@Override
			public boolean count(final long count) {
				logger.debug("sftp bytes: " + count);
				return true;
			}

			@Override
			public void end() {
				logger.debug("sftp done");
			}

		};

		final PathMaker maker = new PathMaker(logger, source, target);

		final List<Entry> entryList = maker.getEntryList();

		ensureTargetFolder(channel, target);

		for (final Entry entry : entryList) {

			final String file = entry.target;
			final int index = file.lastIndexOf("/");
			final String folder = file.substring(0, index);

			ensureTargetFolder(channel, folder);

			channel.put(entry.source, entry.target, monitor,
					ChannelSftp.OVERWRITE);

		}

		//

		channel.disconnect();

		final int status = channel.getExitStatus();

		session.disconnect();

		logger.info("sftp exit status: " + status);

		return status;

	}

	public int retrieve(final String source, final String target)
			throws Exception {

		logger.info("sftp user  : " + user);
		logger.info("sftp host  : " + host);
		logger.info("sftp port  : " + port);
		logger.info("sftp source: " + source);
		logger.info("sftp target: " + target);

		final Session session = newSession();

		final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");

		channel.connect();

		final SftpProgressMonitor monitor = new SftpProgressMonitor() {

			@Override
			public void init(final int op, final String source,
					final String target, final long max) {
				logger.info("sftp download: " + target);
			}

			@Override
			public boolean count(final long count) {
				logger.debug("sftp bytes: " + count);
				return true;
			}

			@Override
			public void end() {
				logger.debug("sftp done");
			}

		};

		//

		final PathFetcher fetcher = new PathFetcher(logger);

		fetcher.fetchFolder(channel, source, target, monitor);

		//

		channel.disconnect();

		final int status = channel.getExitStatus();

		session.disconnect();

		logger.info("sftp exit status: " + status);

		return status;

	}

}
