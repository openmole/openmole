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
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import scala.swing.event.SelectionChanged
import scala.Some
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.implementation.workflow.MoleScene

object ExplorationTaskPanelUI {
  val emptyProxy = SamplingCompositionDataProxyUI()
}

import ExplorationTaskPanelUI._

class ExplorationTaskPanelUI(pud: ExplorationTaskDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends TaskPanelUI with Publisher {

  val samplingComboBox = new ComboBox(comboContent)
  samplingComboBox.selection.item = pud.sampling.getOrElse(emptyProxy)

  val linkLabel = new LinkLabel("", new Action("") {
    def apply =
      if (samplingComboBox.selection.item != emptyProxy) {
        ScenesManager().currentScene match {
          case Some(s: MoleScene) ⇒ ConceptMenu.display(samplingComboBox.selection.item)
          case _                  ⇒
        }
      }
  }) {
    icon = EYE
  }

  val components = List(("Settings", new PluginPanel("wrap 4") {
    contents += new Label("Sampling")
    //add(samplingComboBox, "gapbottom 40")
    contents += samplingComboBox
    contents += linkLabel
  }))

  listenTo(`samplingComboBox`)
  samplingComboBox.selection.reactions += {
    case SelectionChanged(`samplingComboBox`) ⇒
      linkLabel.action = contentAction(samplingComboBox.selection.item)
  }

  def contentAction(proxy: SamplingCompositionDataProxyUI) = new ContentAction(proxy.dataUI.name, proxy) {
    override def apply = ScenesManager().currentSceneContainer.get.scene.displayPropertyPanel(proxy)
  }

  override def saveContent(name: String) =
    new ExplorationTaskDataUI(name, if (samplingComboBox.selection.item == emptyProxy) None else Some(samplingComboBox.selection.item))

  def comboContent: List[SamplingCompositionDataProxyUI] = emptyProxy :: Proxies.instance.samplings.toList

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  add(samplingComboBox,
    new Help(i18n.getString("sampling"), ""))

}
