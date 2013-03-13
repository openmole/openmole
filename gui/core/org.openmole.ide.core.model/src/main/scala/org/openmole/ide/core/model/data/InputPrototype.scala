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
package org.openmole.ide.core.model.data

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI

trait InputPrototype {
  def inputParameters: scala.collection.mutable.Map[IPrototypeDataProxyUI, String]

  def inputParameters_=(ip: scala.collection.mutable.Map[IPrototypeDataProxyUI, String])

  def inputs: List[IPrototypeDataProxyUI]

  def inputs_=(pi: List[IPrototypeDataProxyUI])
}