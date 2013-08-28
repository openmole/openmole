/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
package org.openmole.ide.core.implementation.data

import scala.collection.immutable.HashMap
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

trait InputPrototype {
  def inputParameters: Map[PrototypeDataProxyUI, String]
  def inputs: Seq[PrototypeDataProxyUI]
  def filterInputs(pproxy: PrototypeDataProxyUI): Seq[PrototypeDataProxyUI] = inputs.filterNot(_ == pproxy).toSeq
  def filterInputParameters(pproxy: PrototypeDataProxyUI): Map[PrototypeDataProxyUI, String] = inputParameters.filterNot(_._1 == pproxy)
}