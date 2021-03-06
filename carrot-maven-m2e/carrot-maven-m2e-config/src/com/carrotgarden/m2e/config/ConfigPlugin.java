/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.m2e.config;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;
import org.osgi.framework.Bundle;

public class ConfigPlugin {

	static final String ID = "com.carrotgarden.m2e.config";

	public static void log(final PartInitException exception) {
		// TODO Auto-generated method stub

	}

	public static void log(final MalformedURLException exception) {
		// TODO Auto-generated method stub

	}

	public static Image getImage(final String string) {
		// TODO Auto-generated method stub
		return null;
	}

	static void log(final int status, final String message) {

		final Bundle bundle = Platform.getBundle(ConfigPlugin.ID);

		final ILog log = Platform.getLog(bundle);

		log.log(new Status(status, ID, message));

	}

}
