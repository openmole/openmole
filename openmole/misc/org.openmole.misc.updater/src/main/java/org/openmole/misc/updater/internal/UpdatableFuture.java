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
package org.openmole.misc.updater.internal;

import java.util.concurrent.Future;
import org.openmole.misc.updater.*;

public class UpdatableFuture implements IUpdatableFuture {

    Future future;

    public UpdatableFuture() {
        super();
    }

    @Override
    public synchronized void stopUpdate() {
        future.cancel(true);
    }

    public boolean isCanceled() {
        return future != null && future.isCancelled();
    }

    public synchronized void setFuture(Future future) {
        this.future = future;
    }
}
