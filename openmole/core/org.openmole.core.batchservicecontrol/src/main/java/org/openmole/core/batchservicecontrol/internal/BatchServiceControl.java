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
package org.openmole.core.batchservicecontrol.internal;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.core.batchservicecontrol.BotomlessTokenPool;
import org.openmole.core.batchservicecontrol.IBatchServiceControl;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IAccessTokenPool;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.model.execution.batch.IFailureControl;
import org.openmole.core.model.execution.batch.IUsageControl;
import org.openmole.commons.aspect.eventdispatcher.ObjectModified;
import org.openmole.commons.exception.UserBadDataError;

//TODO manage resources life cycle
public class BatchServiceControl implements IBatchServiceControl {

    Map<IBatchServiceDescription, Duo<IUsageControl, IFailureControl>> ressources = Collections.synchronizedMap(new TreeMap<IBatchServiceDescription, Duo<IUsageControl, IFailureControl>>());
    //Map<IBatchRessourceDescription, IBatchRessourceUsageChangeNotification> notifications = Collections.synchronizedMap(new TreeMap<IBatchRessourceDescription, IBatchRessourceUsageChangeNotification>());
    IAccessTokenPool defaultTokenPool = new BotomlessTokenPool();

    @Override
    public double getFailureRate(IBatchServiceDescription description) {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            return 0.0;
        }
        return control.getRight().getFailureRate();
    }

    @Override
    public void failed(IBatchServiceDescription description) {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            return;
        }
        control.getRight().failed();
    }

    @Override
    public IAccessToken waitAToken(IBatchServiceDescription description) throws InterruptedException {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            return defaultTokenPool.waitAToken();
        }
        return control.getLeft().waitAToken();
    }

    @Override
    public IAccessToken tryGetToken(IBatchServiceDescription description, long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            return defaultTokenPool.waitAToken();
        }
        return control.getLeft().tryGetToken(time, unit);
    }

    @Override
    public boolean contains(IBatchServiceDescription ressource) {
        return ressources.containsKey(ressource);
    }

    @Override
    public void reinitFailure(IBatchServiceDescription ressource, IFailureControl failureControl) {
        Duo<IUsageControl, IFailureControl> control = ressources.get(ressource);
        control.setRight(failureControl);
    }

    @Override
    public synchronized void registerRessouce(IBatchServiceDescription ressource, IUsageControl usageControl, IFailureControl failureControl) {
        if (!contains(ressource)) {
            ressources.put(ressource, new Duo<IUsageControl, IFailureControl>(usageControl, failureControl));
        } /*else {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Ressource with description " + ressource.toString() + " is allready registred.");
        }*/
    }

    @ObjectModified(type = resourceReleased)
    @Override
    public void releaseToken(IBatchServiceDescription description, IAccessToken token) throws InternalProcessingError, UserBadDataError {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            defaultTokenPool.releaseToken(token);
        } else {
            control.getLeft().releaseToken(token);
        }
    }

    @Override
    public void sucess(IBatchServiceDescription description) {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            return;
        }
        control.getRight().success();
    }

    @Override
    public IAccessToken getAccessTokenInterruptly(
            IBatchServiceDescription description) {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            return defaultTokenPool.getAccessTokenInterruptly();
        }
        return control.getLeft().getAccessTokenInterruptly();
    }

    @Override
    public int getLoad(IBatchServiceDescription description) {
        Duo<IUsageControl, IFailureControl> control = ressources.get(description);
        if (control == null) {
            return defaultTokenPool.getLoad();
        }
        return control.getLeft().getLoad();
    }

    /*@Override
    public void registerForNotification(IBatchRessourceDescription ressource, IBatchRessourceUsageChangeNotification notification) {
    notifications.put(ressource, notification);
    }*/
}
