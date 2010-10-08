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

package org.openmole.core.model.domain;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.job.IContext;

/**
 *
 * {@link IRange} is a range of variation for a factor.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 * @param <T> the type of the range
 */
public interface IDiscretizedIntervalDomain<T> extends IFiniteDomain<T> {

    IInterval<? extends T> getInterval();



    /**
     *
     * Get the sting representing the step of discretisation of this domain before evaluation.
     *
     * @return the sting representing  the step of discretisation of this domain befor evaluation
     */
    /*String getStep();

    T getStep(IContext context) throws InternalProcessingError, UserBadDataError;
    T getCenter(IContext context) throws InternalProcessingError, UserBadDataError;
    T getRange(IContext context) throws InternalProcessingError, UserBadDataError;*/
}
