package org.openmole.gui.client.core

import org.openmole.gui.ext.data._
import org.openmole.gui.shared.Api
import autowire._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{ Failure, Success }

/*
 * Copyright (C) 22/12/15 // mathieu.leclaire@openmole.org
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

object CoreUtils {

  implicit class SeqUpdater[T](sequence: Seq[T]) {
    def updatedFirst(t: T, s: T): Seq[T] = {
      val index = sequence.indexOf(t)
      if (index != -1) sequence.updated(index, s)
      else sequence
    }

    def updatedFirst(cond: T ⇒ Boolean, s: T): Seq[T] =
      sequence.find(cond).map { e ⇒ updatedFirst(e, s) }.getOrElse(sequence)
  }

  def withTmpFile(todo: SafePath ⇒ Unit): Unit = {
    OMPost[Api].temporaryFile.call().foreach { tempFile ⇒
      todo(tempFile)
    }
  }
}
