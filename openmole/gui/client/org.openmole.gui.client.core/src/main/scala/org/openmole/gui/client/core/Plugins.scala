package org.openmole.gui.client.core

import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.data.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
/*
 * Copyright (C) 30/11/16 // mathieu.leclaire@openmole.org
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

object Plugins:
  def buildJSObject[T](obj: GUIPluginAsJS) =
    val toBeEval = s"openmole_library.${obj.split('.').takeRight(3).dropRight(1).mkString("_")}"
    util.Try { scalajs.js.eval(toBeEval).asInstanceOf[T] }


