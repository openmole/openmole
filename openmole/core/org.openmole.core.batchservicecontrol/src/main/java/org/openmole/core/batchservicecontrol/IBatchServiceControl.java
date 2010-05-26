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
package org.openmole.core.batchservicecontrol;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.workflow.model.execution.batch.IAccessToken;
import org.openmole.core.workflow.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.workflow.model.execution.batch.IFailureControl;
import org.openmole.core.workflow.model.execution.batch.IUsageControl;
import org.openmole.commons.exception.UserBadDataError;

public interface IBatchServiceControl {

    final static String resourceReleased = "resourceReleased";

    int getLoad(IBatchServiceDescription description);

    IAccessToken waitAToken(IBatchServiceDescription description) throws InterruptedException;

    IAccessToken getAccessTokenInterruptly(IBatchServiceDescription description);

    void releaseToken(IBatchServiceDescription description, IAccessToken token) throws InternalProcessingError, UserBadDataError;

    void failed(IBatchServiceDescription description);

    void sucess(IBatchServiceDescription description);

    void registerRessouce(IBatchServiceDescription ressource, IUsageControl usageControl, IFailureControl failureControl);

    void reinitFailure(IBatchServiceDescription ressource, IFailureControl failureControl);

    boolean contains(IBatchServiceDescription ressource);

    IAccessToken tryGetToken(IBatchServiceDescription description, long time, TimeUnit unit) throws InterruptedException, TimeoutException;

    double getFailureRate(IBatchServiceDescription description);
    //void registerForNotification(IBatchRessourceDescription ressource,IBatchRessourceUsageChangeNotification notification);
}
