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

package org.openmole.ide.plugin.hook.filemanagement

import java.io.File
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
import scala.swing.FileChooser
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.event.ButtonClicked
import org.openmole.plugin.hook.filemanagement.CopyFileHook

class CopyFileHookPanelUI(execution: IMoleExecution, 
                          prototypes: HashMap[IPrototypeDataProxyUI,IPrototype[_]], 
                          capsuleUI: ICapsuleUI, 
                          capsule: ICapsule) extends BoxPanel(Orientation.Vertical) with IHookPanelUI{
 xLayoutAlignment = 0.0F
 yLayoutAlignment = 0.0F
  var currentCopyFileHook= new HashMap[IPrototypeDataProxyUI,Option[CopyFileHook]]
  
  contents += new Label("Save in File: "){xAlignment = Alignment.Left; font = new Font("Ubuntu", BOLD,font.getSize)}
  capsuleUI.dataProxy.get.dataUI.prototypesOut.foreach( 
    p=> {if(p.dataUI.coreObject.`type`.erasure == classOf[File]) {
        contents.append(copyHookPanel(p))
      }})               

  private def copyHookPanel(dpu: IPrototypeDataProxyUI): BoxPanel = {
    val cftf = new FakeTextField("Choose the file path","",FileChooser.SelectionMode.FilesOnly)
    val cb = new CheckBox("Save " + dpu.dataUI.name +" in "){reactions+= {case ButtonClicked(cb) =>
          if (selected) {println("SEL COPY H ");currentCopyFileHook+= dpu-> Some(new CopyFileHook(execution,capsule,prototypes(dpu).asInstanceOf[IPrototype[File]],cftf.text))}
          else currentCopyFileHook(dpu).get.release}}
    listenTo(cb)
    new BoxPanel(Orientation.Horizontal){xLayoutAlignment = 0; contents.append(cb,cftf)}
  }
}