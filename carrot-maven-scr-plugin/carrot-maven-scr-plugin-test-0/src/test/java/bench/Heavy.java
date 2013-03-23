/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package bench;

class Heavy {

	static void log(final String text) {
		System.out.println(text);
	}

	static {
		log("static heavy");
	}

}
