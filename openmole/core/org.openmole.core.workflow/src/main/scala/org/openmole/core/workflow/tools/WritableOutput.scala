package org.openmole.core.workflow.tools

/*
 * Copyright (C) 2019 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io._

import org.openmole.core.expansion.FromContext

object WritableOutput {

  implicit def fromFile(file: FromContext[File]) = FileValue(file)
  implicit def fromPrintStream(ps: PrintStream) = PrintStreamValue(ps)

  case class FileValue(file: FromContext[File]) extends WritableOutput
  case class PrintStreamValue(file: PrintStream) extends WritableOutput

  def file(writableOutput: WritableOutput) =
    writableOutput match {
      case FileValue(file) ⇒ Some(file)
      case _               ⇒ None
    }

}

sealed trait WritableOutput
