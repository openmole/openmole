/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.commons.tools.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

/**
 *
 * @author reuillon
 */
public class FileOutputStream extends java.io.FileOutputStream {

    public FileOutputStream(FileDescriptor fdObj) {
        super(fdObj);
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        super(file, append);
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        super(file);
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        super(name, append);
    }

    public FileOutputStream(String name) throws FileNotFoundException {
        super(name);
    }

    @Override
    protected void finalize() { }
    
}
