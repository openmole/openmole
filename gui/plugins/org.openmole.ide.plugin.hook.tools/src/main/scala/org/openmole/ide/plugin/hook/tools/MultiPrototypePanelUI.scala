/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.plugin.hook.tools

import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import scala.swing.Label

class MultiPrototypePanelUI(title: String,
                            selected: List[IPrototypeDataProxyUI] = List.empty,
                            prototypeProxys: List[IPrototypeDataProxyUI] = List.empty) extends PluginPanel("wrap") {
  val multiPrototypeCombo = new MultiCombo("",
    prototypeProxys,
    selected.map { p ⇒
      new ComboPanel(prototypeProxys,
        new ComboData(Some(p)))
    },
    NO_EMPTY,
    ADD)

  contents += new Label("<html><b>" + title + "</b></html>")
  contents += multiPrototypeCombo.panel
}