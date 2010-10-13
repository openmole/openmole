/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.sampler;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.job.IContext;

/**
 *
 * {@link IPlan} is a strategy for generating a {@link IExploredPlan}.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface ISampler {

	/**
	 * This method builds the explored plan in the givern {@code context}.
	 *
         * @param context context in which the exploration takes place
         * @throws InternalProcessingError  if something goes wrong because of a system failure
         * @throws UserBadDataError         if something goes wrong because it is missconfigured
         * @throws InternalProcessingError  if the thread is interrupted
	 */
	public ISample build(IContext global, IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException;


}