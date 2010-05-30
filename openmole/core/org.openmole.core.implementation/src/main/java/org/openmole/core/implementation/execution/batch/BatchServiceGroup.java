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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.openmole.core.batchservicecontrol.IBatchServiceControl;

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

public class BatchServiceGroup<T extends IBatchService> implements IBatchServiceGroup<T> {

    class BatchRessourceGroupAdapter implements IObjectChangedSynchronousListener<IBatchServiceControl> {

        @Override
        public void objectChanged(IBatchServiceControl obj) throws InternalProcessingError, UserBadDataError {
            ressourceTokenReleased();
        }

    }


   // static final long timeout = 5 * 60 * 1000;
    final ArrayList<T> ressources = new ArrayList<T>();
    double bestRatio;

    transient Semaphore waiting;
    transient Lock selectingRessource;
    
    private double expulseThreshold;

    public BatchServiceGroup(double bestRatio, double threshold) {
        super();
        this.bestRatio = bestRatio;
        this.expulseThreshold = threshold;
        Activator.getEventDispatcher().registerListener(Activator.getBatchRessourceControl(), Priority.NORMAL.getValue(), new BatchRessourceGroupAdapter(), IBatchServiceControl.resourceReleased);
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

    /* (non-Javadoc)
     * @see org.openmole.geclipse.IGeclipseRessourceGroup#getARessource()
     */
    @Override
    public Duo<T, IAccessToken> getAService() throws InterruptedException, InternalProcessingError, UserBadDataError {

        getSelectingRessource().lock();

        try {

            Duo<T, IAccessToken> ret = null;

            while (ret == null) {

                int nbRessources = (int) Math.ceil(size() * bestRatio);

                ArrayList<T> bestRessources = new ArrayList<T>(nbRessources);


                //Select the less failing resources
                synchronized (ressources) {

                    Iterator<T> resourcesIt = ressources.iterator();

                    while (resourcesIt.hasNext()) {
                        IBatchService resource = resourcesIt.next();

                        if (/*ressources.size() > 1 && */Activator.getBatchRessourceControl().getFailureRate(resource.getDescription()) > getExpulseThreshold()) {
                            resourcesIt.remove();
                        }
                    }

                    if(ressources.isEmpty()) throw new InternalProcessingError("No more reliable resource available.");
                    resourcesIt = ressources.iterator();

                    int worstOfBestIndex = 0;

                    bestRessources.add(resourcesIt.next());

                    while (resourcesIt.hasNext()) {
                        T cur = resourcesIt.next();

                        if (bestRessources.size() < nbRessources) {
                            bestRessources.add(cur);
                            double curFailureRate = Activator.getBatchRessourceControl().getFailureRate(cur.getDescription());
                            double worstRessourceFailureRate = Activator.getBatchRessourceControl().getFailureRate(bestRessources.get(worstOfBestIndex).getDescription());

                            if (curFailureRate > worstRessourceFailureRate) {
                                worstOfBestIndex = bestRessources.size() - 1;
                            }
                        } else {
                            if (Activator.getBatchRessourceControl().getFailureRate(cur.getDescription()) < Activator.getBatchRessourceControl().getFailureRate(bestRessources.get(worstOfBestIndex).getDescription())) {
                                bestRessources.set(worstOfBestIndex, cur);
                            }
                        }
                    }
                }

                //Among them select one not over loaded
                Iterator<T> bestResourcesIt = bestRessources.iterator();
                List<Duo<T, IAccessToken>> notLoaded = new ArrayList<Duo<T, IAccessToken>>();

                  
                while (bestResourcesIt.hasNext()) {
                    T cur = bestResourcesIt.next();
                    IAccessToken token = Activator.getBatchRessourceControl().getAccessTokenInterruptly(cur.getDescription());

                    if (token != null) {
                        notLoaded.add(new Duo<T, IAccessToken>(cur, token));
                    }
                }

                if (notLoaded.size() > 0) {
                    ret = notLoaded.remove(RNG.getRng().nextInt(notLoaded.size()));

                    for (Duo<T, IAccessToken> other : notLoaded) {                    
                         Activator.getBatchRessourceControl().releaseToken(other.getLeft().getDescription(), other.getRight());
                        
                    }
                } else {
                    waitForRessourceReleased();
                }

            }

           // Logger.getLogger(BatchServiceGroup.class.getName()).info(ret.getLeft().getDescription() + " " + Activator.getBatchRessourceControl().getFailureRate(ret.getLeft().getDescription()));
            return ret;
        } finally {
            getSelectingRessource().unlock();
        }
    }

    /* (non-Javadoc)
     * @see org.openmole.geclipse.IGeclipseRessourceGroup#add(T)
     */
    public void put(T e) {
//        e.setGroup(this);

        synchronized (ressources) {
            ressources.add(e);
        }
    }

    public T get(int index) {
        synchronized (ressources) {
            return ressources.get(index);
        }
    }

    public boolean isEmpty() {
        return ressources.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.openmole.geclipse.IGeclipseRessourceGroup#size()
     */
    public int size() {
        return ressources.size();
    }

    @Override
    public Iterator<T> iterator() {
        return ressources.iterator();
    }

    private double getExpulseThreshold() {
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
