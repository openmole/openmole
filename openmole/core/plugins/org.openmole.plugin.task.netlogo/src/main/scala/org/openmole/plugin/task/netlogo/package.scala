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

package org.openmole.plugin.task

import org.openmole.core.model.data.Prototype
import org.openmole.core.implementation.builder._

package object netlogo {

  lazy val netLogoInputs = new {
    def +=(p: Prototype[_], n: String): Op[NetLogoTaskBuilder] =
      _.addNetLogoInput(p, n)
    def +=(p: Prototype[_]): Op[NetLogoTaskBuilder] =
      _.addNetLogoInput(p)
  }

  lazy val netLogoOutput = new {
    def +=(n: String, p: Prototype[_]): Op[NetLogoTaskBuilder] =
      _.addNetLogoOutput(n, p)
    def +=(p: Prototype[_]): Op[NetLogoTaskBuilder] =
      _.addNetLogoOutput(p)
    def +=(name: String, column: Int, p: Prototype[_]): Op[NetLogoTaskBuilder] =
      _.addNetLogoOutput(name, column, p)
  }

  trait NetLogoPackage extends external.ExternalPackage {
    lazy val netLogoInputs = netlogo.netLogoInputs
    lazy val netLogoOutput = netlogo.netLogoOutput
  }

}
