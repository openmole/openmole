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

package org.openmole.misc.executorservice.internal;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.misc.workspace.ConfigurationLocation;

public class ExecutorService implements IExecutorService {

    final static ConfigurationLocation NbTread = new ConfigurationLocation(ExecutorService.class.getSimpleName(), "NbThreadsByExecutorTypes");

    final ThreadFactory threadFactory = new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    //t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }
            };
        
    
    static {
        Activator.getWorkspace().$plus$eq(NbTread, "20");
    }

    final Map<String, java.util.concurrent.ExecutorService> executorServices = new TreeMap<String, java.util.concurrent.ExecutorService>();

    int nbThreads;

    public ExecutorService() throws InternalProcessingError, UserBadDataError {
        super();
        nbThreads = Activator.getWorkspace().preferenceAsInt(NbTread);
        //cachedPool = Executors.newCachedThreadPool(threadFactory);
    }

    @Override
    public java.util.concurrent.ExecutorService getExecutorService(ExecutorType type) {
        if (type == ExecutorType.OWN) {
            return Executors.newSingleThreadExecutor(threadFactory);
        }

        return getExecutorService(type.name());
    }

    @Override
    public java.util.concurrent.ExecutorService getExecutorService(String type) {
        java.util.concurrent.ExecutorService ret;

        synchronized (executorServices) {
            ret = executorServices.get(type);
            if (ret == null) {
                ret = Executors.newFixedThreadPool(nbThreads, threadFactory);
                executorServices.put(type, ret);
            }
        }
        return ret;
    }

    @Override
    public void removeAndShutDownExecutorService(String type, boolean now) {
        java.util.concurrent.ExecutorService ret;

        synchronized (executorServices) {
            ret = executorServices.remove(type);
        }
        if (ret != null) {
            if (now) {
                ret.shutdownNow();
            } else {
                ret.shutdown();
            }
        }
    }
}


