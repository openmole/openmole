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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.openmole.misc.executorservice.ExecutorType;

public class Updater implements IUpdater {

    boolean shutDown = false;
    ScheduledExecutorService scheduler;

    public Updater() {

        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setPriority(Thread.MAX_PRIORITY);
                return t;
            }
        });

    }
    
    @Override
    public IUpdatableFuture registerForUpdate(IUpdatableWithVariableDelay updatable, ExecutorType type) {
        UpdatableFuture updatableFuture = new UpdatableFuture();
        UpdaterTask task = new UpdaterTask(updatable, this, updatableFuture, type);
        updatableFuture.setFuture(Activator.getExecutorService().getExecutorService(type).submit(task));
        return updatableFuture;
    }

    @Override
    public IUpdatableFuture delay(final IUpdatableWithVariableDelay updatable, final ExecutorType type) {

        final UpdatableFuture future = new UpdatableFuture();
        final UpdaterTask task = new UpdaterTask(updatable, Updater.this, future, type);

        delay(task);
        return future;
    }
    

    @Override
    public IUpdatableFuture registerForUpdate(IUpdatable updatable, ExecutorType type, long updateInterval) {
        return registerForUpdate(new UpdatableWithFixedDelay(updatable, updateInterval), type);
    }

    @Override
    public IUpdatableFuture delay(final IUpdatable updatable, final ExecutorType type, long updateInterval) {
        return delay(new UpdatableWithFixedDelay(updatable, updateInterval), type);
    }

    public void delay(final UpdaterTask updaterTask) {
        if (!shutDown) {
            UpdatableFuture updatableFuture = updaterTask.getFuture();
            synchronized (updatableFuture) {
                if (!updatableFuture.isCanceled()) {
                    updatableFuture.setFuture(getScheduler().schedule(new Runnable() {

                        @Override
                        public void run() {
                            UpdatableFuture updatableFuture = updaterTask.getFuture();
                            synchronized (updatableFuture) {
                                if (!updatableFuture.isCanceled()) {
                                    updatableFuture.setFuture(Activator.getExecutorService().getExecutorService(updaterTask.getExecutorType()).submit(updaterTask));
                                }
                            }


                        }
                    }, updaterTask.getDelay() , TimeUnit.MILLISECONDS));
                }
            }
        }
    }


    ExecutorService getExecutor(ExecutorType type) {
        return Activator.getExecutorService().getExecutorService(type);
    }


    private ScheduledExecutorService getScheduler() {
        return scheduler;
    }

}
