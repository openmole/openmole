/*
 * 
 *  Copyright 2007, Cemagref
 * 
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */

package org.openmole.commons.exception;

public class UserBadDataError extends Exception {

    private Throwable exception;
    private String message;
    
    public UserBadDataError(Throwable e, String message) {
        this.initCause(e);
        this.exception = e;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public UserBadDataError(Throwable e) {
        this(e, null);
    }

    public UserBadDataError(String string) {
    	this(null,string);
	}

	public Throwable getException() {
        return exception;
    }
    
}
