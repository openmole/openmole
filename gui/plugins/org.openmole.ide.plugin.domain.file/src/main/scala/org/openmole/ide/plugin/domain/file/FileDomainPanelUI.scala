/*
* Copyright (C) 2012 Mathieu Leclaire
* < mathieu.leclaire at openmole.org >
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.openmole.ide.plugin.domain.file

import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.PluginPanel
import swing._

object FileDomainPanelUI {
  def panel(cList: List[(Component, String)]) = {
    val pp = new PluginPanel("fillx", "[left][grow,fill]", "")
    cList.foreach {
      case (c, t) ⇒
        if (!t.isEmpty) pp.contents += (new Label(t), "gap para")
        pp.contents += (c, "wrap")
    }
    pp
  }
}

trait FileDomainPanelUI {
  def directoryTextField(dir: String) = new ChooseFileTextField(dir)
}