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
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.core.model.control.IExecutionManager
import java.awt.Font
import java.awt.Font._
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombosChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiTwoCombosChooseFileTextField._
import scala.swing.Label
import scala.swing.Panel
import scala.swing.event.ButtonClicked
import scala.swing.event.SelectionChanged

object CopyFileHookPanelUI{
  def rowFactory(hookpanel: CopyFileHookPanelUI) = new Factory[IPrototype[File],ICapsule] {
    override def apply(row: TwoCombosChooseFileTextFieldRowWidget[IPrototype[File],ICapsule], p: Panel) = {
      import row._
      val twocombrow= new TwoCombosChooseFileTextFieldRowWidget(name,
                                                                comboContentA,
                                                                selectedA,
                                                                inBetweenString1,
                                                                comboContentB,
                                                                selectedB,
                                                                inBetweenString2,
                                                                filePath,
                                                                plus) {
        override def doOnClose = hookpanel.executionManager.commitHook("org.openmole.plugin.hook.filemanagement.CopyFileHook")
      }
      twocombrow.combo1.selection.reactions += {case SelectionChanged(twocombrow.`combo1`)=>commit}
      twocombrow.combo2.selection.reactions += {case SelectionChanged(twocombrow.`combo2`)=>commit}
      twocombrow.refreshButton.reactions += {case ButtonClicked(twocombrow.`refreshButton`)=>commit}
      
      
      def commit = hookpanel.executionManager.commitHook("org.openmole.plugin.hook.filemanagement.CopyFileHook")
      
      twocombrow
    }
  }
}

import CopyFileHookPanelUI._

class CopyFileHookPanelUI(val executionManager: IExecutionManager) extends MigPanel("wrap") with IHookPanelUI{
  var multiRow : Option[MultiTwoCombosChooseFileTextField[IPrototype[File],ICapsule]] = None
  val capsules : List[ICapsule]= executionManager.capsuleMapping.values.filter(_.outputs.size > 0).toList
  
  if (capsules.size>0) {
    if (protosFromTask(capsules(0)).size>0){
      val r =  new TwoCombosChooseFileTextFieldRowWidget("Save",
                                                         protosFromTask(capsules(0)),
                                                         protosFromTask(capsules(0))(0),
                                                         "from",
                                                         capsules,
                                                         capsules(0),
                                                         "in",
                                                         "",
                                                         NO_ADD)
    
      multiRow =  Some(new MultiTwoCombosChooseFileTextField(List(r),
                                                             rowFactory(this),
                                                             CLOSE_IF_EMPTY,
                                                             NO_ADD))
    }
  }
  
  if (multiRow.isDefined) {
    contents+= (new Label("Save prototypes") {font = new Font("Ubuntu", Font.BOLD, 15)},"left")
    contents+= multiRow.get.panel
  }
    
  def protosFromTask(c: ICapsule): List[IPrototype[File]] = 
    executionManager.prototypeMapping.values.filter(_.`type`.erasure == classOf[File]).map(_.asInstanceOf[IPrototype[File]]).toList
    // To be uncommented when the ComboBox is fixed 
  //  c.outputs.map(_.prototype).filter(_.`type`.erasure == classOf[File]).map(_.asInstanceOf[IPrototype[File]]).toList
  
  
  def saveContent = {
    if (multiRow.isDefined) multiRow.get.content.map{c=>new CopyFileHookDataUI(executionManager,(c._2,c._1,c._3))}
    else List()
  }
  
  def addHook = if (multiRow.isDefined) multiRow.get.showComponent
}

//class CopyFileHookPanelUI(execution: IMoleExecution, 
//                          prototypes: HashMap[IPrototypeDataProxyUI,IPrototype[_]], 
//                          capsuleUI: ICapsuleUI, 
//                          capsule: ICapsule) extends BoxPanel(Orientation.Vertical) with IHookPanelUI{
//  xLayoutAlignment = 0.0F
//  yLayoutAlignment = 0.0F
//  var currentCopyFileHook= new HashMap[IPrototypeDataProxyUI,Option[CopyFileHook]]
//  
//  capsuleUI.dataProxy.get.dataUI.prototypesOut.foreach( 
//    p=> {if(p.dataUI.coreObject.`type`.erasure == classOf[File]) {
//        contents.append(copyHookPanel(p))
//      }})               
//
//  if (contents.size > 0) contents.insert(0,new Label("Save in File: "){xAlignment = Alignment.Left; font = new Font("Ubuntu", BOLD,font.getSize)})
//  private def copyHookPanel(dpu: IPrototypeDataProxyUI): BoxPanel = {
//    val cftf = new ChooseFileTextField("","Select a file path")
//    val cb = new CheckBox("Save " + dpu.dataUI.name +" in "){reactions+= {case ButtonClicked(cb) =>
//          if (selected) {currentCopyFileHook+= dpu-> Some(new CopyFileHook(execution,capsule,prototypes(dpu).asInstanceOf[IPrototype[File]],cftf.text))}
//          else currentCopyFileHook(dpu).get.release}}
//    listenTo(cb)
//    new BoxPanel(Orientation.Horizontal){xLayoutAlignment = 0; contents.append(cb,cftf)}
//  }
//}