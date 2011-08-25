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
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.mutable.HashMap
import java.awt.Font
import java.awt.Font._
import scala.swing.Separator
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.event.ButtonClicked
import org.openmole.ide.misc.widget.MigPanel

class DisplayHookPanelUI(executionManager: IExecutionManager) extends MigPanel("") with IHookPanelUI{
  var toBeHooked = new HashMap[ICapsule,Set[IPrototype[_]]]
  executionManager.capsuleMapping.keys.foreach(c=>{
      if (c.dataProxy.get.dataUI.prototypesOut.size > 0){
        toBeHooked+= executionManager.capsuleMapping(c)-> Set.empty[IPrototype[_]]
        contents+= (new Label(c.dataProxy.get.dataUI.name),"wrap")
        contents+= (new MigPanel(""){
            c.dataProxy.get.dataUI.prototypesOut.foreach(pdu=>
              contents+= displayHookCheckBox(c,pdu))},"wrap")}})
    
  private def displayHookCheckBox(capsuleUI: ICapsuleUI, pdu: IPrototypeDataProxyUI): CheckBox = {
    val cb = new CheckBox(pdu.dataUI.name){reactions+= {case ButtonClicked(cb) =>{
            if (cb.selected) toBeHooked(executionManager.capsuleMapping(capsuleUI))+= executionManager.prototypeMapping(pdu)
            else toBeHooked(executionManager.capsuleMapping(capsuleUI))-= executionManager.prototypeMapping(pdu)
            executionManager.commitHook(DisplayHookPanelUI.this)}}}
    listenTo(cb)
    cb
  }
  
  override def saveContent = new DisplayHookDataUI(executionManager,toBeHooked.toMap)
}