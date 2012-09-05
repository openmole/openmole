/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.task.netlogo

import org.openmole.core.implementation.data._
import org.openmole.core.model.data.Prototype
import org.openmole.plugin.task.external.ExternalTaskBuilder
import scala.collection.mutable.ListBuffer

abstract class NetLogoTaskBuilder extends ExternalTaskBuilder {

  private var _netLogoInputs = new ListBuffer[(Prototype[_], String)]
  private var _netLogoOutputs = new ListBuffer[(String, Prototype[_])]

  def netLogoInputs = _netLogoInputs.toList

  def addNetLogoInput(p: Prototype[_], n: String): this.type = {
    _netLogoInputs += p -> n
    this addInput p
    this
  }

  def addNetLogoInput(p: Prototype[_]): this.type = this.addNetLogoInput(p, p.name)

  def netLogoOutputs = _netLogoOutputs.toList

  def addNetLogoOutput(n: String, p: Prototype[_]): this.type = {
    _netLogoOutputs += n -> p
    this addOutput p
    this
  }

  def addNetLogoOutput(p: Prototype[_]): this.type = this.addNetLogoOutput(p.name, p)

}