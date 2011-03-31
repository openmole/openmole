/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

import org.openmole.core.model.task.IGenericTask
import org.openmole.ui.ide.commons.IOType._
import org.openmole.ui.ide.workflow.implementation.PrototypeUI

trait IGenericTaskModelUI[T<: IGenericTask] extends IObjectModelUI[T] {
  def proceed

  def addPrototype(p: PrototypeUI,ioType: IOType)
  
  def getPrototypesIn: Set[PrototypeUI]
  
  def getPrototypesOut: Set[PrototypeUI]
}
//public interface IGenericTaskModelUI<T extends IGenericTask> extends IObjectModelUI<T>{
//    public void proceed();
//    void addPrototype(PrototypeUI p,IOType ioType);
//    Set<PrototypeUI> getPrototypesIn();
//    Set<PrototypeUI> getPrototypesOut();
//    String getName();
//    Class getType();
//}