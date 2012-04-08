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
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import scala.swing.event.SelectionChanged
import scala.swing.Label
import scala.swing.MyComboBox
import scala.collection.JavaConversions._

class MoleTaskPanelUI(pud: MoleTaskDataUI) extends PluginPanel("fillx,wrap 2","left,grow,fill","") with ITaskPanelUI{
  
  val moleComboBox = new MyComboBox(TopComponentsManager.moleScenes.map{_.manager}.filter{_.capsules.size > 0}.toList) 
  {tooltip = Help.tooltip("The name of the inner Mole to be run.","Mole_1")}
  
  pud.mole match {
    case Some(x:Int) => MoleTaskDataUI.manager(x) match {
        case Some (y : IMoleSceneManager)  => moleComboBox.selection.item = y
        case _ =>
      }
    case _ =>
  }
  
  val capsuleComboBox = new MyComboBox(currentCapsules.toList) 
  {tooltip = Help.tooltip("The name of the final encapsulated task","myTask1")}
  
  contents += (new Label("Embedded mole"),"gap para")
  contents += moleComboBox
  contents += (new Label("Final capsule"),"gap para")
  contents += capsuleComboBox
  
  
  pud.finalCapsule match {
    case Some(x: ITaskDataProxyUI) => 
      MoleTaskDataUI.capsule(x,moleComboBox.selection.item) match {
        case Some(y: ICapsuleDataUI)=>  capsuleComboBox.selection.item = y
        case _=>
      }
    case _=>
  }
  
  listenTo(moleComboBox.selection)
  reactions += {
    case SelectionChanged(`moleComboBox`) => 
      capsuleComboBox.peer.setModel(MyComboBox.newConstantModel(currentCapsules.toList))
  }
  
  def currentCapsules = TopComponentsManager.moleScenes.map{_.manager}.filter(_ == moleComboBox.selection.item).head.capsules.values.filter{_.dataUI.task.isDefined}.map{_.dataUI}
  
  override def saveContent(name: String) = new MoleTaskDataUI(name, Some(moleComboBox.selection.item.id),capsuleComboBox.selection.item.task)
}
