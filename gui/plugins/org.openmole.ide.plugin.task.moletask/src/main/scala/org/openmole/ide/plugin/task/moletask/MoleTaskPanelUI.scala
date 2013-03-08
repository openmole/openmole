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

import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.workflow.MoleSceneManager
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.tools.util._
import scala.swing.TabbedPane
import scala.swing.event.SelectionChanged
import scala.swing.Label
import scala.swing.MyComboBox
import org.openmole.ide.misc.widget.URL
import scala.collection.JavaConversions._

class MoleTaskPanelUI(pud: MoleTaskDataUI) extends PluginPanel("fillx,wrap 2", "left,grow,fill", "") with ITaskPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val moleComboBox = new MyComboBox(MoleTaskDataUI.emptyMoleSceneManager ::
    ScenesManager.moleScenes.map { _.manager }.filter {
      _ != ScenesManager.currentSceneContainer.get.scene.manager
    }.filter {
      _.capsules.size > 0
    }.toList)

  moleComboBox.selection.item = pud.mole match {
    case Some(x: ID.Type) ⇒ MoleTaskDataUI.manager(x) match {
      case Some(m: IMoleSceneManager) ⇒ m.asInstanceOf[MoleSceneManager]
      case _ ⇒ MoleTaskDataUI.emptyMoleSceneManager
    }
    case _ ⇒ MoleTaskDataUI.emptyMoleSceneManager
  }

  val capsuleComboBox = new MyComboBox(EmptyDataUIs.emptyTaskProxy :: currentCapsules.toList)

  capsuleComboBox.selection.item = pud.finalCapsule.getOrElse(EmptyDataUIs.emptyTaskProxy)

  tabbedPane.pages += new TabbedPane.Page("Settings", new PluginPanel("wrap 2") {
    contents += (new Label("Embedded mole"), "gap para")
    contents += moleComboBox
    contents += (new Label("Final capsule"), "gap para")
    contents += capsuleComboBox
  })

  listenTo(moleComboBox.selection)
  reactions += {
    case SelectionChanged(`moleComboBox`) ⇒
      capsuleComboBox.peer.setModel(MyComboBox.newConstantModel(currentCapsules.toList))
  }

  def currentCapsules = {
    val li = ScenesManager.moleScenes.map { _.manager }.filter(_ == moleComboBox.selection.item)
    if (li.size > 0) li.head.capsules.values.filter { _.dataUI.task.isDefined }.map { _.dataUI.task.get }.toList
    else List(EmptyDataUIs.emptyTaskProxy)
  }

  override def saveContent(name: String) =
    new MoleTaskDataUI(name,
      if (moleComboBox.selection.item.id == -1) None else Some(moleComboBox.selection.item.id),
      if (capsuleComboBox.selection.item == EmptyDataUIs.emptyTaskProxy) None else Some(capsuleComboBox.selection.item))

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(moleComboBox,
      new Help(i18n.getString("mole"),
        i18n.getString("moleEx")))
    add(capsuleComboBox,
      new Help(i18n.getString("finalCapsule"),
        i18n.getString("finalCapsuleEx")))
  }
}
