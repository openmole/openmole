package main.scala.org.openmole.gui.server.factory

import org.openmole.core.model.data.Prototype
import org.openmole.gui.ext.data.{ PrototypeData, ErrorData, Data }
import org.openmole.gui.server.factory.ServerFactories

import scala.util.Failure

/*
 * Copyright (C) 26/09/14 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object CoreFactory {

  def check(data: Seq[Data]): Seq[ErrorData] = {
    data.map { d ⇒
      d -> ServerFactories.coreObject(d)
    }.collect { case (data: Data, f: Failure[_]) ⇒ ErrorData(data, f.exception.getMessage, f.exception.getStackTrace.mkString("\n")) }

  }

  // def prototype(prototypeData: PrototypeData): Prototype[_] =
}
