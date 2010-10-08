/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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


package org.openmole.commons.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

public class MultipleException extends Exception implements Iterable<Throwable>{

	Iterable<Throwable> exceptions;

	public MultipleException(Iterable<Throwable> exceptions) {
		super();
		this.exceptions = exceptions;
	}


	@Override
	public void printStackTrace() {
		super.printStackTrace();
		for(Throwable t: exceptions) {
			System.err.println("---------------------------------------");
			t.printStackTrace();
		}
		System.err.println("---------------------------------------");
	}

	@Override
	public void printStackTrace(PrintStream s) {
		super.printStackTrace(s);

		for(Throwable t: exceptions) {
			s.println("----------------------------------------");
			t.printStackTrace(s);
		}
		s.println("----------------------------------------");

	}

	@Override
	public void printStackTrace(PrintWriter s) {
		super.printStackTrace(s);

		for(Throwable t: exceptions) {
			s.println("----------------------------------------");

			t.printStackTrace(s);
		}
		s.println("----------------------------------------");

	}


	public Iterator<Throwable> iterator() {
		return exceptions.iterator();
	}
	
	
	
	
	
	
}
