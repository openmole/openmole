package org.openmole.gui.client.core.files

import java.io.File
import TreeNodeTabs._
import org.openmole.gui.client.core.{ PanelTriggerer, OMPost }
import org.openmole.gui.ext.data.FileExtension._
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

class FileDisplayer(implicit executionTriggerer: PanelTriggerer) {

  val tabs = TreeNodeTabs()

  def alreadyDisplayed(tn: TreeNode) = {
    tabs.tabs.now.find {
      _.treeNode.safePath.now.path == tn.safePath.now.path
    }
  }

  class EditableNodeTabWithOMSTabControl(tn: TreeNode, ed: EditorPanelUI) /*extends EditableNodeTab(tn, ed)*/ extends OMSTabControl(tn, ed) {

    val relativePath = SafePath.empty

    lazy val node = tn

    def onrun = {
      overlaying() = true
      refresh(() ⇒
        OMPost[Api].runScript(ScriptData(tn.safePath.now)).call().foreach { execInfo ⇒
          overlaying() = false
          executionTriggerer.open
        })
    }
  }

  def displayOMS(tn: TreeNode, content: String) = {
    val ed = editor(FileExtension.OMS, content)
    tabs ++ new EditableNodeTabWithOMSTabControl(tn, ed)
  }

  def display(tn: TreeNode, content: String) = {
    val fileType = tn.safePath.now.extension
    alreadyDisplayed(tn) match {
      case Some(t: TreeNodeTab) ⇒ tabs.setActive(t)
      case _ ⇒ fileType match {
        case oms: OpenMOLEScript ⇒ displayOMS(tn, content)
        case md: MDScript ⇒ OMPost[Api].mdToHtml(tn.safePath.now).call.foreach { htmlString ⇒
          tabs ++ new HTMLTab(tn, htmlString)
        }
        case dod: DisplayableOnDemandFile ⇒
          tabs ++ new LockedEditionNodeTab(tn, editor(fileType, content))
        case _ ⇒ //FIXME for GUI workflows
      }
    }
  }

}
