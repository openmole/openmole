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

import org.openmole.ide.misc.widget.{ LinkLabel, PluginPanel }
import scala.swing._
import event.ButtonClicked
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.core.implementation.dataproxy.{ SourceDataProxyUI, HookDataProxyUI, EnvironmentDataProxyUI, Proxies }
import org.openmole.ide.misc.widget.multirow.MultiCombo.{ ComboData, ComboPanel }
import org.openmole.ide.core.implementation.data.{ EnvironmentDataUI, EmptyDataUIs, CapsuleDataUI }
import java.awt.Color
import org.openmole.ide.core.implementation.execution.{ ScenesManager, GroupingStrategyPanelUI }
import org.openmole.ide.core.implementation.panel.Settings

trait CapsulePanelUI extends Publisher with Settings {

  val dataUI: CapsuleDataUI

  type DATAUI = CapsuleDataUI

  val index: Int = 0

  def sources = Proxies.instance.sources.toList
  def hooks = Proxies.instance.hooks.toList

  val sourcePanel = new MultiCombo("",
    sources.toSeq,
    dataUI.sources.map { s ⇒
      new ComboPanel(sources, new ComboData(Some(s)))
    },
    CLOSE_IF_EMPTY,
    ADD)

  val hookPanel = new MultiCombo("",
    hooks.toSeq,
    dataUI.hooks.map { h ⇒
      new ComboPanel(hooks, new ComboData(Some(h)))
    },
    CLOSE_IF_EMPTY,
    ADD)

  val environmentProxys = Proxies.instance.environments :+ EmptyDataUIs.localEnvironmentProxy
  val environmentCombo = new MyComboBox(environmentProxys)

  val groupingCheckBox = new CheckBox("Grouping") { foreground = Color.WHITE }
  val groupingPanel = new GroupingStrategyPanelUI(dataUI.grouping)

  dataUI.environment match {
    case Some(e: EnvironmentDataProxyUI) ⇒ environmentCombo.selection.item = e
    case _                               ⇒ environmentCombo.selection.item = environmentProxys.last
  }

  groupingPanel.visible = dataUI.grouping.isDefined
  groupingCheckBox.selected = dataUI.grouping.isDefined

  val executionPanel = new PluginPanel("wrap") {
    contents += new PluginPanel("wrap 3") {
      contents += new Label("Environment") { foreground = Color.WHITE }
      contents += environmentCombo
      contents += new LinkLabel("", new Action("") {
        def apply =
          if (environmentCombo.selection.index != environmentProxys.size - 1) {
            ScenesManager.displayExtraPropertyPanel(environmentCombo.selection.item)
          }
      }) { icon = org.openmole.ide.misc.tools.image.Images.EYE }
    }
    contents += new PluginPanel("wrap") {
      contents += groupingCheckBox
      contents += groupingPanel
    }
  }

  val components = List(("Source", sourcePanel.panel), ("Hook", hookPanel.panel), ("Execution", executionPanel))

  listenTo(`groupingCheckBox`)
  reactions += {
    case ButtonClicked(`groupingCheckBox`) ⇒ groupingPanel.visible = groupingCheckBox.selected
  }

  def saveContent(n: String) =
    CapsuleDataUI(dataUI.task,
      environmentCombo.selection.item.dataUI match {
        case EmptyDataUIs.LocalEnvironmentDataUI ⇒ None
        case e: EnvironmentDataUI                ⇒ Some(environmentCombo.selection.item)
      },
      if (groupingCheckBox.selected) groupingPanel.save else None,
      sourcePanel.content.map { _.comboValue.get }.filter {
        _ match {
          case s: SourceDataProxyUI ⇒ true
          case _                    ⇒ false
        }
      },
      hookPanel.content.map { _.comboValue.get }.filter {
        _ match {
          case s: HookDataProxyUI ⇒ true
          case _                  ⇒ false
        }
      })
}