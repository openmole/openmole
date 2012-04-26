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
package org.openmole.ide.plugin.task.netlogo

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.DialogClosedEvent
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import java.awt.Dimension
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing._
import swing.Swing._
import org.openmole.ide.osgi.netlogo.NetLogo
import scala.swing.FileChooser._
import java.io.File

abstract class GenericNetLogoPanelUI(nlogoPath: String,
                                     workspaceEmbedded: Boolean,
                                     lauchingCommands: String,
                                     prototypeMappingInput: List[(IPrototypeDataProxyUI, String)],
                                     prototypeMappingOutput: List[(String,IPrototypeDataProxyUI)],
                                     resources : List[String],
                                     g: List[String]) extends PluginPanel("","[left]rel[grow,fill]",
                                                                          "[]20[]"){
 
  val nlogoTextField = new ChooseFileTextField(nlogoPath,"Select a nlogo file","Netlogo files","nlogo")
  val workspaceCheckBox = new CheckBox("Embedd Workspace"){selected = workspaceEmbedded}
  val launchingCommandTextArea = new TextArea(lauchingCommands) 
  var multiStringProto : Option[MultiTwoCombos[String,IPrototypeDataProxyUI]] = None
  var multiProtoString : Option[MultiTwoCombos[IPrototypeDataProxyUI,String]] = None
  val resourcesMultiTextField = new MultiChooseFileTextField("Resources",resources,SelectionMode.FilesAndDirectories)
  var globals = g
  
  listenTo(nlogoTextField)
  reactions += {
    case DialogClosedEvent(nlogoTextField)=> 
      globals = List()
      buildMultis(nlogoTextField.text)}
  
  contents+= new Label("Nlogo file")
  contents+= (nlogoTextField,"growx,wrap")
  contents+= (workspaceCheckBox,"span,growx,wrap")
  contents+= (new Label("Commands"),"wrap")
  contents+= (new ScrollPane(launchingCommandTextArea){minimumSize = new Dimension(150,80)},"span,growx")
  buildMultis(nlogoPath)
  
  def buildMultis(path: String) = {
    if (globals.isEmpty){
      val nl = buildNetLogo
      try{
        if ((new File(path)).isFile){
          nl.open(path)
          globals = nl.globals.toList
          nl.dispose
        }
      }
    }
    if (!globals.isEmpty){
      multiStringProto = Some(new MultiTwoCombos[String,IPrototypeDataProxyUI](
          "Output Mapping",
          "with",
          (globals, comboContent),
          prototypeMappingOutput))
      
      multiProtoString = Some(new MultiTwoCombos[IPrototypeDataProxyUI,String](
          "Input Mapping",
          "with",
          (comboContent,globals),
          prototypeMappingInput))
    }
    
    if (multiStringProto.isDefined) {
      if (contents.size == 8) {contents.remove(5); contents.remove(5); contents.remove(5)}
      contents+= (resourcesMultiTextField.panel,"span,growx,wrap")
      contents+= (multiProtoString.get.panel,"span,grow,wrap")
      contents+= (multiStringProto.get.panel,"span,grow,wrap")
    }
  }
  
  def comboContent: List[IPrototypeDataProxyUI] = new PrototypeDataProxyUI(new EmptyPrototypeDataUI)::Proxys.prototypes.toList

  def buildNetLogo: NetLogo
}
