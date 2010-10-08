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


package org.openmole.misc.updater.internal;

import org.openmole.misc.updater.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.misc.executorservice.ExecutorType;

public class UpdaterTask implements Runnable {

    final private IUpdatableWithVariableDelay updatable;
    final private Updater updater;
    final private ExecutorType type;

    public UpdaterTask(IUpdatableWithVariableDelay updatable, Updater updater, ExecutorType type) {
        super();
        this.updatable = updatable;
        this.updater = updater;
        this.type = type;
    }
    
    @Override
    public void run() {
        try {
            if(updatable.update())   
                updater.delay(this);
        } catch (Throwable e) {
            Logger.getLogger(UpdaterTask.class.getName()).log(Level.WARNING, null, e);
        } 
    }

    public IUpdatableWithVariableDelay getUpdatable() {
        return updatable;
    }

    public ExecutorType getExecutorType() {
        return type;
    }

    public long getDelay() {
        return updatable.getDelay();
    }
}
