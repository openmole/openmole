/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.model.IEntityUI
import scala.collection.mutable.HashSet

class TaskUI(var name: String,val factory: ITaskFactoryUI) extends IEntityUI {
  
  def this(factory: ITaskFactoryUI)= this(MoleScenesManager.incrementTaskName,factory)
  
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