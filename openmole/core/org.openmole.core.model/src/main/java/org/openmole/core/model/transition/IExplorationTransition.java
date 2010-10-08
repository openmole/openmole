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

package org.openmole.core.model.transition;

import org.openmole.core.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactorValues;

/**
 *
 * A transition from a {@link IExplorationTaskCapsule} to a {@link IGenericTaskCapsule}.
 * It create one {@link IMoleJob} for each {@link IFactorValues} of the {@link IExploredPlan}
 * built from the starting {@link IExplorationTaskCapsule}.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IExplorationTransition extends IGenericTransition<IExplorationTaskCapsule> {

}
