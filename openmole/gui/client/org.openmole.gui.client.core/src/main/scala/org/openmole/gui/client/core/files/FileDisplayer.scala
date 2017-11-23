package org.openmole.gui.client.core.files

import TreeNodeTabs._
import org.openmole.gui.ext.data.ScriptData
import org.openmole.gui.ext.data._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import rx._
import org.openmole.gui.client.core.panels._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.client.core._

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

class FileDisplayer(val tabs: TreeNodeTabs) {

  def alreadyDisplayed(safePath: SafePath) =
    tabs.tabs.now.find { t ⇒
      t.safePathTab.now.path == safePath.path
    }

  class EditableNodeTabWithOMSTabControl(sp: SafePath, ed: EditorPanelUI) /*extends EditableNodeTab(tn, ed)*/ extends OMSTabControl(Var(sp), ed) {

    def onrun = {
      refresh(() ⇒
        post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].runScript(ScriptData(safePathTab.now)).call().foreach { execInfo ⇒
          if (computation.now == Pending) executionPanel.dialog.show
          standby
        })
    }
  }

  def displayOMS(safePath: SafePath, content: String) = {
    val ed = editor(FileExtension.OMS, content)
    tabs ++ new EditableNodeTabWithOMSTabControl(safePath, ed)
  }

  def display(safePath: SafePath, content: String, fileExtension: FileExtension) = {
    alreadyDisplayed(safePath) match {
      case Some(t: TreeNodeTab) ⇒
        tabs.setActive(t)
      case _ ⇒ fileExtension match {
        case OpenMOLEScript ⇒ displayOMS(safePath, content)
        case MDScript ⇒ post()[Api].mdToHtml(safePath).call.foreach { htmlString ⇒
          tabs ++ new HTMLTab(Var(safePath), htmlString)
        }
        case SVGExtension ⇒ tabs ++ new HTMLTab(Var(safePath), content)
        case dod: EditableOnDemandFile ⇒
          tabs ++ new LockedEditionNodeTab(Var(safePath), editor(fileExtension, content))
        case _ ⇒ //FIXME for GUI workflows
      }
    }
  }

}
