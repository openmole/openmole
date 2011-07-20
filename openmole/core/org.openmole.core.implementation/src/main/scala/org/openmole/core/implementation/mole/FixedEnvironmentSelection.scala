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

package org.openmole.core.implementation.mole

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.mole.IEnvironmentSelection
import scala.collection.mutable.HashMap

object FixedEnvironmentSelection {  
  val Empty = new FixedEnvironmentSelection(new HashMap())
}

class FixedEnvironmentSelection(environments: HashMap[ICapsule, IEnvironment]) extends IEnvironmentSelection {
   
  def this() = this(new HashMap[ICapsule, IEnvironment])

  override def select(capsule: ICapsule): Option[IEnvironment] = environments.get(capsule);
    
  def select(capsule: ICapsule, environment: IEnvironment) {
    environments(capsule) = environment
  }

}
