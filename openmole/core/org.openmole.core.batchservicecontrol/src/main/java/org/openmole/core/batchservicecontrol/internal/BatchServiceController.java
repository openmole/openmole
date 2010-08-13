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
package org.openmole.core.batchservicecontrol.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.batchservicecontrol.BotomlessTokenPool;
import org.openmole.core.batchservicecontrol.IBatchServiceController;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IAccessTokenPool;
import org.openmole.core.batchservicecontrol.IFailureControl;
import org.openmole.core.batchservicecontrol.IUsageControl;

/**
 *
 * @author reuillon
 */
public class BatchServiceController implements IBatchServiceController {

    static final IBatchServiceController defaultController = new BatchServiceController(new IUsageControl() {

        IAccessTokenPool defaultTokenPool = new BotomlessTokenPool();

        @Override
        public IAccessToken tryGetToken(long milliSeconds, TimeUnit unit) throws InterruptedException, TimeoutException {
            return defaultTokenPool.waitAToken(milliSeconds, unit);
        }

        @Override
        public IAccessToken waitAToken() throws InterruptedException {
            return defaultTokenPool.waitAToken();
        }

        @Override
        public void releaseToken(IAccessToken token) throws InternalProcessingError {
            defaultTokenPool.releaseToken(token);
        }

        @Override
        public IAccessToken getAccessTokenInterruptly() {
            return defaultTokenPool.getAccessTokenInterruptly();
        }

        @Override
        public int getLoad() {
            return defaultTokenPool.getLoad();
        }
    }, new IFailureControl() {

        @Override
        public void failed() {
        }

        @Override
        public void success() {
        }

        @Override
        public double getFailureRate() {
            return 0.0;
        }

        @Override
        public void reinit() {
        }
    });
    final IUsageControl usageControl;
    final IFailureControl failureControl;

    public BatchServiceController(IUsageControl usageControl, IFailureControl failureControl) {
        this.usageControl = usageControl;
        this.failureControl = failureControl;
    }

    @Override
    public IUsageControl getUsageControl() {
        return usageControl;
    }

    @Override
    public IFailureControl getFailureControl() {
        return failureControl;
    }

}
