/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.model.panel.ITaskPanelUI
import scala.swing.event.SelectionChanged
import scala.swing.Label
import scala.swing.ComboBox
import scala.collection.JavaConversions._

class MoleTaskPanelUI(pud: MoleTaskDataUI) extends PluginPanel("fillx,wrap 2","","") with ITaskPanelUI{
  
  val moleComboBox = new ComboBox(TopComponentsManager.moleScenes.toList) 
  {tooltip = Help.tooltip("The name of the inner Mole to be run.","Mole_1")}
  val capsuleComboBox = new ComboBox(moleComboBox.selection.item.manager.capsules.values.toList) 
  
  contents += (new Label("Embedded mole"),"gap para")
  contents += moleComboBox
  pud.mole match {
    case Some(x:IMoleScene) => moleComboBox.selection.item = x
    case _ =>
  }
  contents += (new Label("Final capsule"),"gap para")
  contents += capsuleComboBox
  
 // listenTo(moleComboBox.selection)
 // reactions += {
 //   case SelectionChanged(`moleComboBox`) => capsuleComboBox.peer.setModel(ComboBox.newConstantModel(moleComboBox.selection.item.manager.capsules.values.toList)
 // }
 override def saveContent(name: String) = new MoleTaskDataUI(name, Some(moleComboBox.selection.item),Some(capsuleComboBox.selection.item))
}
