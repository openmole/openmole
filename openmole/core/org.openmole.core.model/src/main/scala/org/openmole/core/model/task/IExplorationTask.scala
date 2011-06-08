/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.task

import org.openmole.core.model.sampling.ISampling

/**
 *
 * The interface representing a task for exploring a space of parameter. This task is contained in a {@link IExplorationTaskCapsule}.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
trait IExplorationTask extends IGenericTask {
   def sampling: ISampling
}
