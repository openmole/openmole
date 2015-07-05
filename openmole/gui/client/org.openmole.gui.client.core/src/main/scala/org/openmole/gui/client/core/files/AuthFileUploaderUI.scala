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

import java.net.URI

import org.openmole.gui.client.core.{ Settings, OMPost }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import bs._
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import scalatags.JsDom.all._
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.HTMLInputElement
import rx._

class AuthFileUploaderUI(keyName: String) {

  val fileName = if (keyName == "") uuID else keyName

  val fileView = bs.input("")(
    placeholder := "Not set yet",
    width := "130px",
    readonly,
    value := keyName,
    autofocus
  ).render

  val upButton = bs.uploadButton2((fileInput: HTMLInputElement) ⇒ {
    val fileList = fileInput.files
    Settings.authenticationKeysPath.foreach { tg ⇒
      val targetDirectory = new URI(tg).getPath
      FileManager.upload(fileList,
        targetDirectory,
        (p: FileTransferState) ⇒ {},
        () ⇒ {
          OMPost[Api].renameFileFromPath(targetDirectory + fileList.item(0).name, fileName).call().foreach { b ⇒
            fileView.value = fileName
          }
        }
      )
    }
  }
  )

  val view = bs.inputGroup(navbar_left)(
    Rx {
      inputGroup(navbar_left)(
        inputGroupButton(fileView),
        inputGroupAddon(id := "fileinput-addon") /*(`class` := "inrow")*/ (upButton)
      )
    }
  ).render

}
