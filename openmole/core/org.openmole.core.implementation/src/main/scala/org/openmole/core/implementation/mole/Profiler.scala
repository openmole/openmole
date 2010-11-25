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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.observer.MoleExecutionObserverAdapter
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.IProfiler
import org.openmole.core.implementation.observer. IMoleExecutionObserver


abstract class Profiler extends IProfiler with IMoleExecutionObserver {  
  def register(moleExecution: IMoleExecution) = new MoleExecutionObserverAdapter(moleExecution, this)
}
