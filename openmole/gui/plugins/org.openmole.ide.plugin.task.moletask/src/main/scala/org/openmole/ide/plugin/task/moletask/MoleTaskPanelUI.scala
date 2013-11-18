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

package org.openmole.ide.plugin.task.moletask

import org.openmole.ide.misc.widget._
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.misc.tools.util._
import scala.swing.Label
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.event.SelectionChanged
import org.openmole.ide.misc.widget.multirow.MultiCombo.{ ComboData, ComboPanel }
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.core.implementation.workflow.MoleUI

class MoleTaskPanelUI(pud: MoleTaskDataUI010)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("fillx,wrap 2", "left,grow,fill", "") with TaskPanelUI {

  val moleComboBox = ContentComboBox[ID](currentMoles,
    ScenesManager().moleScenes.toList.map { _.dataUI }.find { m ⇒ Some(m.id) == pud.mole })

  val capsuleComboBox = ContentComboBox(currentCapsules, pud.finalCapsule)

  val multiImplicits = new MultiCombo("Implicits",
    Proxies.instance.prototypes.toList,
    pud.implicits.map { i ⇒
      new ComboPanel(Proxies.instance.prototypes.toList,
        new ComboData(Some(i)))
    }.toSeq,
    minus = CLOSE_IF_EMPTY,
    plus = ADD)

  val components = List(("Settings", new PluginPanel("wrap 2") {
    contents += (new Label("Embedded mole"), "gap para")
    contents += moleComboBox.widget
    contents += (new Label("Final capsule"), "gap para")
    contents += capsuleComboBox.widget
  }), ("Implicits", multiImplicits.panel))

  listenTo(moleComboBox.widget.selection)
  reactions += {
    case SelectionChanged(_) ⇒
      capsuleComboBox.setModel(currentCapsules)
  }

  def currentMoles: List[MoleUI] = (for {
    dataUI ← ScenesManager().moleScenes.map { _.dataUI }
    if dataUI != ScenesManager().currentSceneContainer.get.scene.dataUI
    if dataUI.capsules.size > 0
  } yield dataUI).toList

  def currentCapsules = (for {
    dataUI ← ScenesManager().moleScenes.map { _.dataUI }
    if { Some(dataUI.id) == moleComboBox.widget.selection.item.content.map { _.id } }
    capsule ← dataUI.capsules.values
    task ← capsule.dataUI.task
  } yield task).toList

  override def saveContent(name: String) =
    new MoleTaskDataUI010(name,
      moleComboBox.widget.selection.item.content.map { _.id },
      capsuleComboBox.widget.selection.item.content,
      multiImplicits.content.flatMap { _.comboValue })

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  add(moleComboBox.widget,
    new Help(i18n.getString("mole"),
      i18n.getString("moleEx")))
  add(capsuleComboBox.widget,
    new Help(i18n.getString("finalCapsule"),
      i18n.getString("finalCapsuleEx")))

}
