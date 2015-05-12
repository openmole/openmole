package org.openmole.gui.client.core.files

import java.io.File
import org.openmole.gui.client.core.dataui.EditorPanelUI
import FileExtension._
import org.openmole.gui.misc.js.Tabs
import org.openmole.gui.misc.js.Tabs._
import rx._

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

  val tabs = Tabs()

  def alreadyDisplayed(tn: TreeNode) = {
    tabs.tabs().find {
      _.id == tn.id
    }
  }

  def display(tn: TreeNode, content: String) = {
    val (_, fileType) = FileExtension(tn)
    alreadyDisplayed(tn) match {
      case Some(t: Tab) ⇒ tabs.setActive(t)
      case _            ⇒ tabs.addTab(Tab(tn.name(), editor(fileType, content).view, tn.id))
    }
  }
}
