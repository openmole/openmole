/*
 *  Copyright (c) 2008, Cemagref
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
package org.openmole.misc.workspace.internal;

import java.io.File;
import java.io.IOException;

public class TempDir {

	File base;
	
	TempDir (File base) {
		this.base = base;
	}

	
    /**
     * Creates a temp directory in the System temp directory.
     */
    public synchronized File createNewDir(String prefix) throws IOException {
        File tempFile = File.createTempFile(prefix, "", base);

        if (!tempFile.delete()) {
            throw new IOException();
        }
        if (!tempFile.mkdir()) {
            throw new IOException();
        }
        
        tempFile.deleteOnExit();
        return tempFile;
    }
   
    
    public File createNewFile(String prefix, String suffix) throws IOException {
    	File tempFile = File.createTempFile(prefix, suffix, base);
        return tempFile;
    }

    public File getLocation() {
        return base;
    }
    
}


