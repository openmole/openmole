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

package org.openmole.core.implementation.execution.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchService;
import org.openmole.core.model.execution.batch.IBatchServiceGroup;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.structure.Priority;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.service.RNG;
import org.openmole.core.batchservicecontrol.IUsageControl;

public class BatchServiceGroup<T extends IBatchService> implements IBatchServiceGroup<T> {

    class BatchRessourceGroupAdapter implements IObjectChangedSynchronousListener<IUsageControl> {

        @Override
        public void objectChanged(IUsageControl obj) throws InternalProcessingError, UserBadDataError {
            ressourceTokenReleased();
        }

    }

    final ArrayList<T> resources = new ArrayList<T>();

    transient Semaphore waiting;
    transient Lock selectingRessource;
    
    private int expulseThreshold;

    public BatchServiceGroup(int threshold) {
        super();
        this.expulseThreshold = threshold;
    }

    private synchronized Semaphore getWaiting() {
        if (waiting == null) {
            waiting = new Semaphore(0);
        }
        return waiting;
    }

    void waitForRessourceReleased() throws InterruptedException {
        getWaiting().acquire();
    }

    @Override
    public Duo<T, IAccessToken> getAService() throws InterruptedException, InternalProcessingError, UserBadDataError {

        getSelectingRessource().lock();
        try {
            Duo<T, IAccessToken> ret = null;

            while (ret == null) {
                ArrayList<T> resourcesCopy; 

                //Select the less failing resources
                synchronized (resources) {

                    Iterator<T> resourcesIt = resources.iterator();

                    while (resourcesIt.hasNext()) {
                        IBatchService resource = resourcesIt.next();

                        if (Activator.getBatchRessourceControl().getController(resource.getDescription()).getFailureControl().getFailureRate() > getExpulseThreshold()) {
                            resourcesIt.remove();
                        }
                    }

                    if(resources.isEmpty()) throw new InternalProcessingError("No more reliable resource available.");
                    
                    resourcesCopy = new ArrayList<T>(resources.size());
                    resourcesCopy.addAll(resources);
                }

                //Among them select one not over loaded
                Iterator<T> bestResourcesIt = resourcesCopy.iterator();
                List<Duo<T, IAccessToken>> notLoaded = new ArrayList<Duo<T, IAccessToken>>();

                while (bestResourcesIt.hasNext()) {
                    
                    T cur = bestResourcesIt.next();

                    IAccessToken token = Activator.getBatchRessourceControl().getController(cur.getDescription()).getUsageControl().getAccessTokenInterruptly();

                    if (token != null) {
                        notLoaded.add(new Duo<T, IAccessToken>(cur, token));
                    }
                }
               
                if (notLoaded.size() > 0) {
                    ret = notLoaded.remove(RNG.getRng().nextInt(notLoaded.size()));

                    for (Duo<T, IAccessToken> other : notLoaded) {                    
                         Activator.getBatchRessourceControl().getController(other.getLeft().getDescription()).getUsageControl().releaseToken(other.getRight());     
                    }
                } else {
                    waitForRessourceReleased();
                }

            }
           return ret;
        } finally {
            getSelectingRessource().unlock();
        }
    }

    @Override
    public void put(T e) {
       synchronized (resources) {
            resources.add(e);
            IUsageControl usageControl = Activator.getBatchRessourceControl().getController(e.getDescription()).getUsageControl();
            Activator.getEventDispatcher().registerListener(usageControl, Priority.NORMAL.getValue(), new BatchRessourceGroupAdapter(), IUsageControl.resourceReleased);
        }
        ressourceTokenReleased();
    }

    public T get(int index) {
        synchronized (resources) {
            return resources.get(index);
        }
    }

    public boolean isEmpty() {
        return resources.isEmpty();
    }

    @Override
    public int size() {
        return resources.size();
    }

    @Override
    public Iterator<T> iterator() {
        return resources.iterator();
    }

    private int getExpulseThreshold() {
        return expulseThreshold;
    }

    public Lock getSelectingRessource() {
        if (selectingRessource != null) {
            return selectingRessource;
        }
        synchronized (this) {
            if (selectingRessource == null) {
                selectingRessource = new ReentrantLock();
            }
        }
        return selectingRessource;
    }


    private void ressourceTokenReleased() {
        getWaiting().release();
    }
}
