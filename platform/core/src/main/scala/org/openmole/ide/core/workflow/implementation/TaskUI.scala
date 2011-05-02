/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import java.awt.Color
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.PanelUI
import org.openmole.ide.core.workflow.model.IEntityUI
import scala.collection.mutable.HashSet

class TaskUI(var name: String, val coreClass: Class[_],val borderColor: Color,val backgroundColor: Color,val panel: PanelUI) extends IEntityUI {
  def this(coreClass: Class[_],borderColor: Color, backgroundColor: Color,panel: PanelUI)= this(MoleScenesManager.incrementTaskName,coreClass,borderColor,backgroundColor,panel)
  
  var prototypesIn= HashSet.empty[PrototypeUI]
  var prototypesOut= HashSet.empty[PrototypeUI]
  
  def addPrototype(p: PrototypeUI, ioType: IOType.Value)= {
    if (ioType.equals(IOType.INPUT)) addPrototypeIn(p)
    else addPrototypeOut(p)
  }

  def addPrototypeIn(p: PrototypeUI)= prototypesIn+= p
  
  def addPrototypeOut(p: PrototypeUI)= prototypesOut+= p
}
//
//public class TaskUI extends EntityUI{
//
//    public TaskUI() {
//    super();
//    }
//    
//    public TaskUI(String name, Class type) {
//        super(name,type);
//    }
//}