/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.panelsettings

import java.awt.Dimension
import scala.swing.{ Label, RadioButton, TabbedPane }
import org.openmole.misc.workspace.Workspace
import org.openmole.ide.core.implementation.workflow.MoleUI
import org.openmole.ide.core.implementation.panel.{ MultiProxies, AnonSaveSettings, Settings }
import scala.swing.TabbedPane.Page
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.core.implementation.registry.PrototypeKey

trait MolePanelUI extends Settings with AnonSaveSettings {

  def dataUI: MoleUI

  type DATAUI = MoleUI

  val tabbed = new TabbedPane {
    minimumSize = new Dimension(300, 400)
    preferredSize = new Dimension(300, 400)
  }

  val pluginPanel = new PluginPanel("wrap") {
    Workspace.pluginDirLocation.list.foreach {
      f ⇒
        contents += new RadioButton(f) {
          selected = dataUI.plugins.toList.contains(f)
        }
    }
  }

  val contextPanel = MultiProxies.comboLinkGroovyEditor(Proxies.instance.prototypes,
    dataUI.implicits.map { _._1 }.toSeq,
    dataUI.implicits.map { p ⇒ p._1 -> p._2 }.toMap)

  tabbed.pages += new Page("Plugins", pluginPanel)
  tabbed.pages += new Page("Context", {
    if (Proxies.instance.prototypes.isEmpty) new Label("Define Prototype first")
    else contextPanel.panel
  })

  lazy val components = List(("", tabbed))

  def saveContent: DATAUI = {
    dataUI.plugins = pluginPanel.contents.flatMap {
      c ⇒
        c match {
          case x: RadioButton ⇒ List(x)
          case _              ⇒ Nil
        }
    }.toList.filter {
      _.selected
    }.map {
      _.text
    }
    dataUI.implicits = contextPanel.content.map { data ⇒ PrototypeKey.build(data.prototype) -> data.editorValue }
    dataUI
  }
}
