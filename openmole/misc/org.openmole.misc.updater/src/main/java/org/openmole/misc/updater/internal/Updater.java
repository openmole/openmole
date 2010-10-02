/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import java.util.concurrent.ExecutorService;
import org.openmole.misc.updater.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.openmole.misc.executorservice.ExecutorType;

public class Updater implements IUpdater {

    boolean shutDown = false;
    final ScheduledExecutorService scheduler;

    public Updater() {

        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

    }

    @Override
    public void registerForUpdate(IUpdatableWithVariableDelay updatable, ExecutorType type) {
        UpdaterTask task = new UpdaterTask(updatable, this, type);
        Activator.getExecutorService().getExecutorService(type).submit(task);
    }

    @Override
    public void delay(final IUpdatableWithVariableDelay updatable, final ExecutorType type) {
        final UpdaterTask task = new UpdaterTask(updatable, Updater.this, type);
        delay(task);
    }

    @Override
    public void registerForUpdate(IUpdatable updatable, ExecutorType type, long updateInterval) {
        registerForUpdate(new UpdatableWithFixedDelay(updatable, updateInterval), type);
    }

    @Override
    public void delay(final IUpdatable updatable, final ExecutorType type, long updateInterval) {
        delay(new UpdatableWithFixedDelay(updatable, updateInterval), type);
    }

    public void delay(final UpdaterTask updaterTask) {
        if (!shutDown) {

            getScheduler().schedule(new Runnable() {

                @Override
                public void run() {
                    Activator.getExecutorService().getExecutorService(updaterTask.getExecutorType()).submit(updaterTask);
                }
            }, updaterTask.getDelay(), TimeUnit.MILLISECONDS);

        }

    }

    ExecutorService getExecutor(ExecutorType type) {
        return Activator.getExecutorService().getExecutorService(type);
    }

    private ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
