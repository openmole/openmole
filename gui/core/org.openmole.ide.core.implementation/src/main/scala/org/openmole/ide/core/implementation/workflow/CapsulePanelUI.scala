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
package org.openmole.ide.core.implementation.workflow

import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.model.panel.{ ICapsulePanelUI, IPanelUI }
import scala.swing._
import org.openmole.ide.core.model.data.{ IEnvironmentDataUI, ICapsuleDataUI }
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.misc.widget.multirow.MultiCombo.{ ComboData, ComboPanel }
import org.openmole.ide.core.implementation.data.{ EmptyDataUIs, CapsuleDataUI }
import org.openmole.ide.core.model.dataproxy.{ IHookDataProxyUI, ISourceDataProxyUI, IEnvironmentDataProxyUI }
import org.openmole.ide.core.implementation.execution.MultiGenericGroupingStrategyPanel
import java.awt.Color

class CapsulePanelUI(dataUI: ICapsuleDataUI) extends PluginPanel("") with ICapsulePanelUI {

  val sources = Proxys.sources.toList
  val hooks = Proxys.hooks.toList

  val sourcePanel = new MultiCombo("",
    sources,
    dataUI.sources.map { s ⇒
      new ComboPanel(sources, new ComboData(Some(s)))
    },
    CLOSE_IF_EMPTY,
    ADD)

  val hookPanel = new MultiCombo("",
    hooks,
    dataUI.hooks.map { h ⇒
      new ComboPanel(hooks, new ComboData(Some(h)))
    },
    CLOSE_IF_EMPTY,
    ADD)

  val environmentProxys = Proxys.environments :+ EmptyDataUIs.emptyEnvironmentProxy
  val environmentCombo = new MyComboBox(environmentProxys)

  dataUI.environment match {
    case Some(e: IEnvironmentDataProxyUI) ⇒ environmentCombo.selection.item = e
    case _ ⇒ environmentCombo.selection.item = environmentProxys.last
  }

  tabbedPane.pages += new TabbedPane.Page("Source", sourcePanel.panel)
  tabbedPane.pages += new TabbedPane.Page("Hook", hookPanel.panel)
  tabbedPane.pages += new TabbedPane.Page("Environment", new PluginPanel("wrap") {
    contents += environmentCombo
    contents += new Label("Grouping: ") { foreground = Color.WHITE }
    contents += (new MultiGenericGroupingStrategyPanel).panel
  })

  def save =
    new CapsuleDataUI(dataUI.task,
      environmentCombo.selection.item.dataUI match {
        case e: EmptyDataUIs.EmptyEnvironmentDataUI ⇒ None
        case e: IEnvironmentDataUI ⇒ Some(environmentCombo.selection.item)
      },
      sourcePanel.content.map { _.comboValue.get }.filter {
        _ match {
          case s: ISourceDataProxyUI ⇒ true
          case _ ⇒ false
        }
      },
      hookPanel.content.map { _.comboValue.get }.filter {
        _ match {
          case s: IHookDataProxyUI ⇒ true
          case _ ⇒ false
        }
      })
}