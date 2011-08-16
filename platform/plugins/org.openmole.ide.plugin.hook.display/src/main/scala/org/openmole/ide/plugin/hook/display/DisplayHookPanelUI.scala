/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.hook.display

import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.mutable.HashMap
import scala.swing.Alignment
import java.awt.Font
import java.awt.Font._
import scala.swing.BoxPanel
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.event.ButtonClicked
import org.openmole.plugin.hook.display.ToStringHook

class DisplayHookPanelUI(execution: IMoleExecution, 
                         prototypes: HashMap[IPrototypeDataProxyUI,IPrototype[_]], 
                         capsuleUI: ICapsuleUI, 
                         capsule: ICapsule) extends BoxPanel(Orientation.Vertical) with IHookPanelUI{
  xLayoutAlignment = 0.0F
  yLayoutAlignment = 0.0F
                         
  private var currentDisplayHook= new HashMap[IPrototypeDataProxyUI,Option[ToStringHook]]
  contents += new Label("Display: "){xAlignment = Alignment.Left; font = new Font("Ubuntu", BOLD,font.getSize)}
  capsuleUI.dataProxy.get.dataUI.prototypesOut.foreach(pdu=> contents += displayHookCheckBox(pdu))
    
  private def displayHookCheckBox(dpu: IPrototypeDataProxyUI): CheckBox = {
    val cb = new CheckBox(dpu.dataUI.name){reactions+= {case ButtonClicked(cb) =>{
            if (cb.selected) currentDisplayHook+= dpu-> Some(new ToStringHook(execution, capsule, prototypes(dpu)))
            else currentDisplayHook(dpu).get.release}}}
    listenTo(cb)
    cb
  }
}