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
package org.openmole.ide.core.implementation.serializer

import org.openmole.ide.core.implementation.workflow.MoleUI
import org.openmole.ide.core.implementation.builder.MoleFactory
import java.io.{ ByteArrayOutputStream, File }
import scala.util.{ Failure, Success }
import org.openmole.core.serializer.SerialiserService
import org.openmole.ide.core.implementation.dialog.StatusBar

object ExecutionSerialiser {

  def apply(moleUI: MoleUI, path: String, withArchive: Boolean = false) = {
    if (withArchive) SerialiserService.serialiseAndArchiveFiles(execution(moleUI), new File(path))
    else SerialiserService.serialise(execution(moleUI), new File(path))
  }

  def apply(moleUI: MoleUI, withArchive: Boolean) = {
    val array = new ByteArrayOutputStream
    SerialiserService.serialise(execution(moleUI), array)
    array.toByteArray
  }

  private def execution(moleUI: MoleUI) =
    MoleFactory.buildMoleExecution(moleUI) match {
      case Success(mE) ⇒ mE._1
      case Failure(t)  ⇒ StatusBar().warn("The mole can not be serialized due to " + t.getMessage)
    }

}