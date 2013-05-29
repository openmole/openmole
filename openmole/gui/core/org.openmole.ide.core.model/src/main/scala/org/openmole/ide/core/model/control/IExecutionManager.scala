/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import java.io.PrintStream
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.data.IHookDataUI

trait IExecutionManager {
  def mole: IMole

  def moleExecution: Option[IMoleExecution]

  def prototypeMapping: Map[IPrototypeDataProxyUI, Prototype[_]]

  def capsuleMapping: Map[ICapsuleUI, ICapsule]

  def printStream: PrintStream

  def start
}
