package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.misc.js.OMTags
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import bs._
import rx._

/*
 * Copyright (C) 20/01/16 // mathieu.leclaire@openmole.org
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

object FileToolBar {

  sealed trait SelectedTool {
    def glyph: String
  }

  object TrashTool extends SelectedTool {
    val glyph = glyph_trash
  }

  object FilterTool extends SelectedTool {
    val glyph = OMTags.glyph_filter
  }

  object FileCreationTool extends SelectedTool {
    val glyph = glyph_plus
  }

  object PluginTool extends SelectedTool {
    val glyph = OMTags.glyph_plug
  }

  object CopyTool extends SelectedTool {
    val glyph = OMTags.glyph_copy
  }

}

import FileToolBar._

class FileToolBar(treeNodePanel: TreeNodePanel) {

  val selectedTool: Var[Option[SelectedTool]] = Var(None)

  def click(tool: SelectedTool)(action: ⇒ Unit) = {
    action
    selectedTool() = Some(tool)
  }

  def rxClass(sTool: SelectedTool) = Rx {
    "glyphicon " + sTool.glyph + " glyphmenu " + selectedTool().filter(_ == sTool).map { _ ⇒ "selectedTool" }.getOrElse("")
  }

  def buildSpan(selectedTool: SelectedTool, action: ⇒ Unit) = OMTags.glyphSpan(rxClass(selectedTool))(
    click(selectedTool) {
      action
    }
  )

  def refresh = CoreUtils.refreshCurrentDirectory()

  val div = bs.div("centerFileTool")(
    glyphSpan(glyph_refresh + " glyphmenu", () ⇒ CoreUtils.refreshCurrentDirectory()),
    glyphSpan(glyph_upload + " glyphmenu", () ⇒ println("upload")),
    buildSpan(PluginTool, println("plug")),
    buildSpan(TrashTool, println("trash")),
    buildSpan(CopyTool, println("topy")),
    buildSpan(FileCreationTool, println("plus")),
    buildSpan(FilterTool, println("filter"))
  )
}
