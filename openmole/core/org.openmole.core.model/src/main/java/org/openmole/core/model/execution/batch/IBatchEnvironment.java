/*
 *  Copyright (C) 2010 Romain Reuillon
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
package org.openmole.core.model.execution.batch;

import java.io.File;
import java.util.Collection;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.IEnvironment;
import scala.Tuple2;

public interface IBatchEnvironment<JS extends IBatchJobService> extends IEnvironment<IBatchExecutionJob> {

    File getRuntime();

    IBatchEnvironmentDescription getDescription();

    IBatchEnvironmentAuthentication getAuthentication() throws InternalProcessingError;

    Collection<JS> allJobServices() throws InternalProcessingError, UserBadDataError;

    Collection<IBatchStorage> allStorages() throws InternalProcessingError, UserBadDataError;

    IBatchServiceGroup<JS> getJobServices() throws InternalProcessingError, UserBadDataError, InterruptedException;

    IBatchServiceGroup<IBatchStorage> getStorages() throws InternalProcessingError, UserBadDataError, InterruptedException;

    Tuple2<JS, IAccessToken> getAJobService() throws InternalProcessingError, UserBadDataError, InterruptedException;

    Tuple2<IBatchStorage, IAccessToken> getAStorage() throws InternalProcessingError, UserBadDataError, InterruptedException;
}
