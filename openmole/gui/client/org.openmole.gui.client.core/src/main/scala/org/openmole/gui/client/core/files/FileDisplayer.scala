package org.openmole.gui.client.core.files

import java.io.File
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
      _.serverFilePath() == tn.safePath()
    }
  }

  def display(rootPath: SafePath, tn: TreeNode, content: String, executionTriggerer: PanelTriggerer) = {
    val fileType = tn.safePath().extension
    alreadyDisplayed(tn) match {
      case Some(t: TreeNodeTab) ⇒ tabs.setActive(t)
      case _ ⇒ fileType match {
        case oms: OpenMOLEScript ⇒
          val ed = editor(fileType, content)
          tabs ++ new EditableNodeTab(tn.name, tn.safePath, ed) with OMSTabControl {
            val relativePath = SafePath.empty

            def onrun = () ⇒ {
              overlaying() = true
              OMPost[Api].runScript(ScriptData(tn.name(), ed.code, "outstream")).call().foreach { execInfo ⇒
                overlaying() = false
                executionTriggerer.open
              }
            }
          }

        case disp: DisplayableFile ⇒ tabs ++ new EditableNodeTab(tn.name, tn.safePath, editor(fileType, content))
        case _                     ⇒ //FIXME for GUI workflows
      }
    }
  }

}
