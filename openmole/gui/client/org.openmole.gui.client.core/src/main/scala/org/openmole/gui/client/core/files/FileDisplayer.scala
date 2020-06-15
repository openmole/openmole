package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.client.core._
import org.openmole.gui.client.tool.plot.Plotter

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

class FileDisplayer(val treeNodeTabs: TreeNodeTabs, showExecution: () ⇒ Unit) {

  def alreadyDisplayed(safePath: SafePath) =
    treeNodeTabs.tabs.now.find { t ⇒
      t.safePathTab.now.path == safePath.path
    }

  def display(safePath: SafePath, content: String, fileExtension: FileExtension) = {

    alreadyDisplayed(safePath) match {
      case Some(t: TreeNodeTab) ⇒ treeNodeTabs.setActive(t)
      case _ ⇒
        fileExtension match {
          case OpenMOLEScript ⇒
            val tab = TreeNodeTab.oms(safePath, content, showExecution, TreeNodeTabs.setErrors(treeNodeTabs, safePath, _))
            treeNodeTabs add tab
            tab.omsEditor.editor.focus
          case OpenMOLEResult ⇒
            Post()[Api].findAnalysisPlugin(safePath).call.foreach {
              case Some(plugin) ⇒
                val analysis = Plugins.buildJSObject[MethodAnalysisPlugin](plugin)
                val tab = TreeNodeTab.html(safePath, analysis.panel(safePath))
                treeNodeTabs add tab
              case None ⇒
            }
          case MDScript ⇒
            Post()[Api].mdToHtml(safePath).call().foreach { htmlString ⇒
              treeNodeTabs add TreeNodeTab.html(safePath, TreeNodeTab.mdBlock(htmlString))
            }
          case SVGExtension ⇒ treeNodeTabs add TreeNodeTab.html(safePath, TreeNodeTab.rawBlock(content))
          case editableFile: EditableFile ⇒
            if (DataUtils.isCSV(safePath))
              Post()[Api].sequence(safePath).call().foreach { seq ⇒
                treeNodeTabs add TreeNodeTab.editable(safePath, content, DataTab.build(seq, view = TreeNodeTab.Table, editing = !editableFile.onDemand), Plotter.default)
              }
            else {
              treeNodeTabs add TreeNodeTab.editable(safePath, content, DataTab.build(SequenceData(Seq(), Seq()), view = TreeNodeTab.Raw), Plotter.default)
            }
          case _ ⇒ //FIXME for GUI workflows
        }
    }
  }

}
