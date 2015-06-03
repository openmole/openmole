package org.openmole.gui.client.core.files

import java.io.File
import FileExtension._
import TreeNodeTabs._
import org.openmole.gui.client.core.{ PanelTriggerer, ExecutionPanel, OMPost }
import org.openmole.gui.ext.data.ScriptData
import org.openmole.gui.shared.Api
import org.openmole.gui.ext.data._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import scalatags.JsDom.tags
import scalatags.JsDom.all._

/*
 * Copyright (C) 07/05/15 // mathieu.leclaire@openmole.org
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

object FileDisplayer {
  def editor(fileType: FileExtension, initCode: String): EditorPanelUI = EditorPanelUI(fileType, initCode)

}

import FileDisplayer._

class FileDisplayer {

  val tabs = TreeNodeTabs()

  def alreadyDisplayed(tn: TreeNode) = {
    tabs.tabs().find {
      _.serverFilePath() == tn.canonicalPath()
    }
  }

  def display(rootPath: String, tn: TreeNode, content: String, executionTriggerer: PanelTriggerer) = {
    val (_, fileType) = FileExtension(tn)
    alreadyDisplayed(tn) match {
      case Some(t: TreeNodeTab) ⇒ tabs.setActive(t)
      case _ ⇒ fileType match {
        case disp: DisplayableFile ⇒ disp match {
          case oms: OpenMOLEScript ⇒
            val ed = editor(fileType, content)
            tabs ++ new EditableNodeTab(tn.name, tn.canonicalPath, ed) with OMSTabControl {
              val script = ed.code
              val relativePath = tn.canonicalPath().split('/').dropRight(1).mkString("/") diff rootPath

              def onrun = () ⇒ {
                overlaying() = true
                OMPost[Api].runScript(ScriptData(tn.name(), script, inputDirectory, outputDirectory, "output")).call().foreach { execInfo ⇒
                  overlaying() = false
                  executionTriggerer.open
                }
              }
            }
          case _ ⇒ tabs ++ new EditableNodeTab(tn.name, tn.canonicalPath, editor(fileType, content))
        }
        case _ ⇒ //FIXME for GUI workflows
      }
    }
  }
}
