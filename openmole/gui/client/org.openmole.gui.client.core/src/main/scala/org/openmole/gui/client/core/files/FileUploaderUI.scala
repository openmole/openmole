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

import org.openmole.gui.client.core.OMPost
import org.openmole.gui.misc.utils.Utils
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.ext.data.{ ProcessState, UploadAuthentication, SafePath }
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import scalatags.JsDom.all._
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.{ HTMLInputElement }
import rx._

class AuthFileUploaderUI(
    keyName:  String,
    keySet:   Boolean,
    renaming: Option[String] = None
) {

  val fileName = if (keyName == "") renaming.getOrElse(Utils.getUUID) else keyName
  val pathSet: Var[Boolean] = Var(keySet)

  val view = upButton.render

  lazy val upButton = tags.label(`class` := "inputFileStyle spacer5 certificate")(
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
            OMPost[Api].renameKey(leaf, fileName).call().foreach {
              b ⇒
                pathSet() = true
            }
          }
        }
      )
    }), Rx {
      if (pathSet()) fileName else "No certificate"
    }
  )
}