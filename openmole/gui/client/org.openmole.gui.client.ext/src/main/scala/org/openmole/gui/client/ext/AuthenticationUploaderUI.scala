package org.openmole.gui.client.ext

/*
 * Copyright (C) 03/07/15 // mathieu.leclaire@openmole.org
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

import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.shared.data.*
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*

object AuthenticationUploaderUI:
  def apply(existingKey: Option[SafePath], directory: SafePath) =
    val up = new AuthenticationUploaderUI(directory)
    up.file.set(existingKey)
    up

class AuthenticationUploaderUI(directory: SafePath):
  val file: Var[Option[SafePath]] = Var(None)

  def view(using api: ServerAPI, path: BasePath) =
    label(
      fileInput: fInput ⇒
        println(fInput.ref.files)
        fInput.ref.files.headOption.foreach: f =>
          val to = directory / f.name
          api.upload:
            fInput.ref.files.toSeq.map(f => f -> to)
          .map: _ ⇒
            file.set(Some(to))
            fInput.ref.value = "",
      child <-- file.signal.map:
        case Some(f) => span(f.name, badge_success, cls := "badgeOM")
        case _ => span("No certificate", badge_secondary, cls := "badgeOM"),
      cls := "inputFileStyle"
    )
