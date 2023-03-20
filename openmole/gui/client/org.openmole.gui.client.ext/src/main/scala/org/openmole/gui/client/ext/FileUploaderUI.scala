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
import org.openmole.gui.shared.data.DataUtils

object FileUploaderUI {
  def empty = FileUploaderUI("", false)
}

case class FileUploaderUI(
  keyName: String,
  keySet: Boolean,
  renaming: Option[String] = None) {

  val fileName = if (keyName == "") renaming.getOrElse(DataUtils.uuID) else keyName
  val pathSet: Var[Boolean] = Var(keySet)

  def view(using api: ServerAPI, path: BasePath) = label(
    fileInput((fInput: Input) ⇒ {
      def to(name: String) = SafePath(Seq(name), ServerFileSystemContext.Authentication)

      api.upload(fInput.ref.files.toSeq.map(f => f -> to(fileName))).map { _ ⇒
//          if fInput.ref.files.length > 0
//          then
//            val leaf = fInput.ref.files.item(0).name
//            import org.openmole.gui.shared.data.ServerFileSystemContext.Authentication
//            val from = SafePath(Seq(leaf), Authentication)
//            val to = SafePath(Seq(fileName), Authentication)
//            pathSet.set(false)
//            api.move(Seq(from -> to)).foreach { _ ⇒ pathSet.set(true) }
        pathSet.set(false)
        fInput.ref.value = ""
      }
    }),
    child <-- pathSet.signal.map { ps ⇒
      if (ps) span(fileName, badge_success, cls := "badgeOM")
      else span("No certificate", badge_secondary, cls := "badgeOM")
    },
    cls := "inputFileStyle"
  )
}