package org.openmole.gui.server.core

import org.openmole.gui.ext.data.{ DataBag, ErrorData }

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

  def check(dataBag: Seq[DataBag]): Seq[ErrorData] = {
    dataBag.map { d ⇒
      d -> ServerFactories.coreObject(d)
    }.collect { case (data: DataBag, f: Failure[_]) ⇒ ErrorData(data, f.exception.getMessage, f.exception.getStackTrace.mkString("\n")) }

  }

  // def prototype(prototypeData: PrototypeData): Prototype[_] =
}
