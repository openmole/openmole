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
package org.openmole.core.batchservicecontrol;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openmole.commons.aspect.eventdispatcher.ObjectModified;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IAccessTokenPool;

public class UsageControl implements IUsageControl {

    IAccessTokenPool tokenPool;

    public UsageControl(int nbAccess) {
        super();
        if (nbAccess != Integer.MAX_VALUE) {
            tokenPool = new AccessTokenPool(nbAccess);
        } else {
            tokenPool = new BotomlessTokenPool();
        }
    }

    @Override
    public IAccessToken tryGetToken(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        return tokenPool.waitAToken(time, unit);
    }

    @Override
    public IAccessToken waitAToken() throws InterruptedException {
        return tokenPool.waitAToken();
    }

    @Override
    public IAccessToken getAccessTokenInterruptly() {
        return tokenPool.getAccessTokenInterruptly();
    }

    @ObjectModified(name = resourceReleased)
    @Override
    public void releaseToken(IAccessToken token) throws InternalProcessingError, UserBadDataError {
        tokenPool.releaseToken(token);
    }

    @Override
    public int getLoad() {
        return tokenPool.getLoad();
    }
}
