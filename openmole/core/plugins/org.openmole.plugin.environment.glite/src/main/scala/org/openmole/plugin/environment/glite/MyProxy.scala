/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.glite

import fr.iscpif.gridscale.glite.{ MyProxy ⇒ GSMyProxy }

object MyProxy {
  def apply(time: String, host: String, port: Option[Int] = None) = new MyProxy(time, host, port)
}

class MyProxy(val time: String, val host: String, override val port: Option[Int] = None) extends GSMyProxy {
  def url = host + port.map(":" + _).mkString
}
