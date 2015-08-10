package org.openmole.gui.client.core.files

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

import org.openmole.gui.client.core.{ Settings, OMPost }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.ext.data.{ UploadAuthentication, SafePath, FileExtension }
import org.openmole.gui.ext.data.SafePath._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import bs._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.ext.data.FileExtension._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import scalatags.JsDom.all._
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.HTMLInputElement
import rx._

class AuthFileUploaderUI(keyName: String, keySet: Boolean, renaming: Option[String] = None) {

  val fileName = if (keyName == "") renaming.getOrElse(uuID) else keyName
  val pathSet: Var[Boolean] = Var(keySet)

  lazy val upButton =
    tags.label(`class` := "inputFileStyle marginTop-20")(
      bs.fileInput((fInput: HTMLInputElement) ⇒ {
        FileManager.upload(fInput,
          SafePath.empty,
          (p: FileTransferState) ⇒ {},
          UploadAuthentication(),
          () ⇒ {
            val leaf = fInput.files.item(0).name
            pathSet() = false
            OMPost[Api].renameKey(leaf, fileName).call().foreach { b ⇒
              pathSet() = true
            }
          }
        )
      }
      ), Rx {
        if (pathSet()) fileName else "No certificate"
      }
    )

  val view = upButton.render

}
