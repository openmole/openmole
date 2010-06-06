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
package org.openmole.core.replicacatalog.internal;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.core.replicacatalog.IReplica;

/**
 *
 * @author reuillon
 */
public class ReplicaCatalogGC implements IUpdatable {

    long updateInterval;
    final ReplicaCatalog catalog;

    public ReplicaCatalogGC(ReplicaCatalog catalog, long updateInterval) {
        this.catalog = catalog;
        this.updateInterval = updateInterval;
    }

    @Override
    public void update() throws InternalProcessingError, UserBadDataError, InterruptedException {

        for (IReplica replica : catalog.getAllReplicas()) {

            //May be the env pluggin is not loaded in this case a version of the description class is persisted by db4o
            if (Activator.getBatchEnvironmentAuthenticationRegistry().isRegistred(replica.getEnvironmentDescription())) {
                boolean initialized = Activator.getBatchEnvironmentAuthenticationRegistry().getAuthentication(replica.getEnvironmentDescription()).isAccessInitialized();
                if (!replica.getSource().exists() && initialized) {
                    catalog.clean(replica);
                }
            }
        }

    }

    @Override
    public long getUpdateInterval() {
        return updateInterval;
    }
}
