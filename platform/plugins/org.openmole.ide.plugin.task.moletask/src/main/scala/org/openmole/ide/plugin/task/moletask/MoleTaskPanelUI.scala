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
import org.openmole.ide.misc.widget.PluginPanel
import org.openide.windows.WindowManager
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.model.panel.ITaskPanelUI
import scala.swing.Label
import scala.swing.ComboBox
import org.openide.util.Utilities

class MoleTaskPanelUI(pud: MoleTaskDataUI) extends PluginPanel("fillx,wrap 2","","") with ITaskPanelUI{
  
  // building the combobox
  val mole = {
    val currentMoleScene = Utilities.actionsGlobalContext().lookupResult(classOf[IMoleScene]).allInstances.toArray(new Array[IMoleScene](0))(0)
    val otherMoleScenes:Array[IMoleScene] = WindowManager.getDefault.findMode("editor").getTopComponents
    .filter(_.isInstanceOf[MoleSceneTopComponent]).map(_.asInstanceOf[MoleSceneTopComponent].getMoleScene)
    .filter(_!=null).filter(_!=currentMoleScene)
    println("current : "+currentMoleScene.manager.name.get)
    otherMoleScenes.foreach{ms=>println(ms.manager.name.get)}
    new ComboBox[IMoleScene](otherMoleScenes)
  }
  contents+= (new Label("Embedded mole"),"gap para")
  contents+= mole
  // selecting the current mole
  if (pud.mole!=None) mole.selection.item = pud.mole
  
  override def saveContent(name: String) = new MoleTaskDataUI(name, mole.selection.item)
}
