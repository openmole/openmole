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
import java.io.InputStream;
import java.util.concurrent.Callable;

public class ReaderRunnable implements Callable<Integer> {

    final byte[] buffer;
    final InputStream from;
    final int maxRead;

    public ReaderRunnable(InputStream from, byte[] buffer, int maxRead) {
        super();
        this.from = from;
        this.maxRead = maxRead;
        this.buffer = buffer;
    }

    @Override
    public Integer call() throws IOException {
         return from.read(buffer, 0, maxRead);
    }

}
