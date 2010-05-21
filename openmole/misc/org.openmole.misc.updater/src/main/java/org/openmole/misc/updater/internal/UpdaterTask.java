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

import org.openmole.misc.updater.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.misc.executorservice.ExecutorType;

public class UpdaterTask implements Runnable {

    private IUpdatable updatable;
    private Updater updater;
    private ExecutorType type;
    private UpdatableFuture future;

    public UpdaterTask(IUpdatable updatable, Updater updater, UpdatableFuture future, ExecutorType type) {
        super();
        this.updatable = updatable;
        this.updater = updater;
        this.future = future;
        this.type = type;
    }

    public void run() {
        try {
            updatable.update();
        } catch (Throwable e) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, null, e);
        } finally {
            //if (future.shouldUpdate()) {
                updater.delay(this);
            //}
        }
    }

    public UpdatableFuture getFuture() {
        return future;
    }

    public IUpdatable getUpdatable() {
        return updatable;
    }

    public ExecutorType getExecutorType() {
        return type;
    }
    /*public static  UpdaterTask getNew() throws InternalProcessingError {
    return updaterPool.getNew();
    }*/

    /*public static void release(UpdaterTask object) {
    updaterPool.release(object);
    }*/
}
