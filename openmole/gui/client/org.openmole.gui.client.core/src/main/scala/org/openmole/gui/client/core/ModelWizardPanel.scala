package org.openmole.gui.client.core

/*
 * Copyright (C) 07/10/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.files._
import org.openmole.gui.ext.data.{ UploadProject, SafePath }
import org.openmole.gui.misc.js.OMTags
import autowire._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import org.scalajs.dom.raw.HTMLInputElement
import org.openmole.gui.misc.js.JsRxTags._
import rx._
import org.openmole.gui.shared.Api
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import bs._

class ModelWizardPanel extends ModalPanel {
  lazy val modalID = "modelWizardPanelID"

  def onOpen() = {}

  def onClose() = {}

  val transferring: Var[FileTransferState] = Var(Standby())
  //val modelName: Var[Option[String]] = Var(None)
  val labelName: Var[Option[String]] = Var(None)

  lazy val upButton = tags.label(`class` := "inputFileStyle spacer5 certificate")(
    bs.fileInput((fInput: HTMLInputElement) ⇒ {
      FileManager.upload(fInput,
        manager.current.safePath(),
        (p: FileTransferState) ⇒ {
          transferring() = p
        },
        UploadProject(),
        () ⇒ {
          if (fInput.files.length > 0) {
            val fileName = fInput.files.item(0).name
            OMPost[Api].getCareBinInfos(manager.current.safePath() ++ fileName).call().foreach { b ⇒
              labelName() = Some(fileName)
            }
            //     }
          }
        }
      )
    }), Rx {
      labelName() match {
        case Some(s: String) ⇒ s
        case _               ⇒ "Your Model"
      }
    }
  )

  val dialog = bs.modalDialog(modalID,
    headerDialog(
      tags.span(tags.b("Model import"))
    ),
    bodyDialog(
      tags.div(
        Rx {
          transferring() match {
            case _: Transfering ⇒ OMTags.waitingSpan(" Uploading ...", btn_danger + "certificate")
            case _: Transfered ⇒
              panels.treeNodePanel.refreshCurrentDirectory
              upButton
            case _ ⇒ upButton
          }
        }
      )
    ),
    footerDialog(closeButton)
  )

}
