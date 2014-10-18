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

import org.openmole.misc.tools.service.OS

package object systemexec {

  case class Commands(parts: Seq[String], os: OS = OS())

  implicit def stringToCommands(s: String) = Commands(Seq(s))
  implicit def seqOfStringToCommands(s: Seq[String]) = Commands(s)
  implicit def tupleToCommands(t: (String, OS)) = Commands(Seq(t._1), t._2)
  implicit def tupleSeqToCommands(t: (Seq[String], OS)) = Commands(t._1, t._2)
}
