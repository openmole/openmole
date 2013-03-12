/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.task.exploration

import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.tools.image.Images._
import org.openmole.ide.misc.widget.URL
import scala.swing._
import scala.swing.event.SelectionChanged
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dataproxy._
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.dataproxy.ISamplingCompositionDataProxyUI
import org.openmole.ide.core.model.panel.ITaskPanelUI

object ExplorationTaskPanelUI {
  val emptyProxy = new SamplingCompositionDataProxyUI
}

import ExplorationTaskPanelUI._

class ExplorationTaskPanelUI(pud: ExplorationTaskDataUI) extends PluginPanel("wrap 3") with ITaskPanelUI {
  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val samplingComboBox = new ComboBox(comboContent)

  tabbedPane.pages += new TabbedPane.Page("Settings", new PluginPanel("wrap 2") {
    contents += new Label("Sampling")
    add(samplingComboBox, "gapbottom 40")
  })

  val linkLabel: LinkLabel = new LinkLabel("", contentAction(pud.sampling.getOrElse(emptyProxy))) {
    icon = EYE
  }
  samplingComboBox.selection.item = pud.sampling.getOrElse(emptyProxy)
  listenTo(`samplingComboBox`)
  samplingComboBox.selection.reactions += {
    case SelectionChanged(`samplingComboBox`) ⇒
      linkLabel.action = contentAction(samplingComboBox.selection.item)
  }
  contents += linkLabel

  def contentAction(proxy: ISamplingCompositionDataProxyUI) = new ContentAction(proxy.dataUI.name, proxy) {
    override def apply = ScenesManager.currentSceneContainer.get.scene.displayExtraPropertyPanel(proxy, EXTRA)
  }

  override def saveContent(name: String) =
    new ExplorationTaskDataUI(name, if (samplingComboBox.selection.item == emptyProxy) None else Some(samplingComboBox.selection.item))

  def comboContent: List[ISamplingCompositionDataProxyUI] = emptyProxy :: Proxys.samplings.toList

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(samplingComboBox,
      new Help(i18n.getString("sampling"), ""))
  }
}
