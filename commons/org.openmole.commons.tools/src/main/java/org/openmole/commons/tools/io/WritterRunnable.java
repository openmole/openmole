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
package org.openmole.commons.tools.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public class WritterRunnable implements Callable<Void> {

    final OutputStream to;
    final byte[] buffer;
    final int amount;

    public WritterRunnable(OutputStream to, byte[] buffer, int amount) {
        super();
        this.to = to;
        this.buffer = buffer;
        this.amount = amount;
    }

    @Override
    public Void call() throws IOException {
        to.write(buffer, 0, amount);
        return null;
    }
}
