package org.openmole.ide.plugin.environment.glite
/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

object Converters {

  implicit def intToString(i: Option[Int]) = i match {
    case Some(ii: Int) ⇒ ii.toString
    case _             ⇒ ""
  }

  implicit def stringToStringOpt(s: String) = s.isEmpty match {
    case true  ⇒ None
    case false ⇒ Some(s)
  }

  implicit def stringToIntOpt(s: String) = try {
    Some(s.toInt)
  }
  catch {
    case e: NumberFormatException ⇒ None
  }
}
