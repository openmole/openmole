package org.openmole.gui.ext.tool.client

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

import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.all._
import scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.client.JsRxTags._
import org.scalajs.dom.raw.HTMLInputElement
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._

import scalatags.JsDom.all._

object FileUploaderUI {
  def empty = FileUploaderUI("", false)
}

case class FileUploaderUI(
  keyName: String,
  keySet:  Boolean,

  renaming: Option[String] = None
) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val fileName = if (keyName == "") renaming.getOrElse(java.util.UUID.randomUUID.toString) else keyName
  val pathSet: Var[Boolean] = Var(keySet)

  val view = upButton

  lazy val upButton = label(
    bs.fileInput((fInput: HTMLInputElement) ⇒ {
      FileManager.upload(
        fInput,
        SafePath.empty,
        (p: ProcessState) ⇒ {
        },
        UploadAuthentication(),
        () ⇒ {
          if (fInput.files.length > 0) {
            val leaf = fInput.files.item(0).name
            pathSet() = false
            OMPost()[Api].renameKey(leaf, fileName).call().foreach {
              b ⇒
                pathSet() = true
            }
          }
        }
      )
    }), Rx {
      if (pathSet()) fileName else "No certificate"
    }
  )(sheet.paddingTop(5) +++ omsheet.certificate +++ "inputFileStyle" +++ sheet.marginTop(40))
}