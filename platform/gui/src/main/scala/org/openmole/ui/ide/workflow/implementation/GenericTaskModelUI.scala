/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.core.model.task.IGenericTask
import org.openmole.ui.ide.commons.IOType
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI
import scala.collection.mutable.HashSet

class GenericTaskModelUI[T <: IGenericTask](taskUI: TaskUI) extends ObjectModelUI with IGenericTaskModelUI[T] {
  
  var prototypesIn= new HashSet[PrototypeUI]
  var prototypesOut= new HashSet[PrototypeUI]
  
  override def addPrototype(p: PrototypeUI, ioType: IOType.Value)= {
    if (ioType.equals(IOType.INPUT)) addPrototypeIn(p)
    else addPrototypeOut(p)
  }

  override def addPrototypeIn(p: PrototypeUI)= prototypesIn+= p
  
  override def addPrototypeOut(p: PrototypeUI)= prototypesOut+= p
}
//
//public abstract class GenericTaskModelUI<T extends IGenericTask> extends ObjectModelUI implements IGenericTaskModelUI {
//
//    private Set<PrototypeUI> prototypesIn;
//    private Set<PrototypeUI> prototypesOut;
//    private final static String category = "Tasks";
//    TaskUI taskUI;
//
//    public GenericTaskModelUI(TaskUI taskUI) {
//        this.taskUI = taskUI;
//    }
//
//   @Override
//    public void addPrototype(PrototypeUI p,
//                             IOType ioType){
//        if (ioType == IOType.INPUT) addPrototypeIn(p);
//        else addPrototypeOut(p);
//    }
//
//    private void addPrototypeIn(PrototypeUI p){
//        if (prototypesIn == null) prototypesIn = new HashSet<PrototypeUI>();
//        prototypesIn.add(p);
//    }
//
//     private void addPrototypeOut(PrototypeUI p){
//        if (prototypesOut == null) prototypesOut = new HashSet<PrototypeUI>();
//        prototypesOut.add(p);
//    }
//  
//    @Override
//    public Class getType(){
//        return taskUI.getType();
//    }
//    
//    @Override
//    public String getName(){
//        return taskUI.getName();
//    }
//   
//    @Override
//    public Set<PrototypeUI> getPrototypesIn() {
//        if (prototypesIn == null) prototypesIn = new HashSet<PrototypeUI>();
//        return prototypesIn;
//    }
//
//    @Override
//    public Set<PrototypeUI> getPrototypesOut() {
//        if (prototypesOut == null) prototypesOut = new HashSet<PrototypeUI>();
//        return prototypesOut;
//    }
//   
//
//    @Override
//    public String getCategory() {
//        return category;
//    }
//}