/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.openmole.commons.tools.io;

import java.io.IOException;
import java.io.OutputStream;

public class WritterRunnable  implements Runnable{

		int amountRead;
		IOException exception;
		OutputStream to;
		byte[] buffer;
		
		public WritterRunnable(OutputStream to, byte[] buffer) {
			super();
			this.to = to;
			this.buffer = buffer;
		}

		void setAmountRead(int amountRead) {
			this.amountRead = amountRead;
		}

		@Override
		public void run() {
			try {
				exception = null;
				to.write(buffer, 0, amountRead);
				to.flush();
			} catch (IOException e) {
				exception = e;
//				Logger.getLogger(FastCopy.class.getName()).log(Level.WARNING, null, e);
			}
		}

		IOException getException() {
			return exception;
		}


		
		

}
