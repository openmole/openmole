/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.control

import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import java.io.PrintStream

trait IExecutionManager {
  def mole: IMole
  
  def moleExecution: IMoleExecution
  
  def prototypeMapping: Map[IPrototypeDataProxyUI,IPrototype[_]]
  
  def capsuleMapping: Map[ICapsuleDataUI, ICapsule]
  
  def printStream: PrintStream
  
  def commitHook(hookName: String)
}
