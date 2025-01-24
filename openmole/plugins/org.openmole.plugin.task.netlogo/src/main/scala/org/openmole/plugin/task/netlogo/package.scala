/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.task.netlogo

import org.openmole.core.context.Val
import org.openmole.core.dsl._

import org.openmole.core.setter.*

class NetLogoInputs:
  def +=[T: MappedInputBuilder : InputOutputBuilder](p: Val[?], n: String): T ⇒ T = inputs += p mapped n
  def +=[T: MappedInputBuilder : InputOutputBuilder](p: Val[?]): T ⇒ T = this.+=[T](p, p.name)

@deprecated
lazy val netLogoInputs = new NetLogoInputs

class NetLogoOutputs:
  def +=[T: MappedOutputBuilder : InputOutputBuilder](n: String, p: Val[?]): T ⇒ T = outputs += p mapped n
  def +=[T: MappedOutputBuilder : InputOutputBuilder](p: Val[?]): T ⇒ T = this.+=[T](p.name, p)

@deprecated
lazy val netLogoOutputs = new NetLogoOutputs

export org.openmole.plugin.task.netlogo.{NetLogoContainerTask as NetLogoTask}
