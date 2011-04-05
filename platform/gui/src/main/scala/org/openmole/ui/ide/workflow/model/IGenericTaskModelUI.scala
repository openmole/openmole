/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

import org.openmole.ui.ide.commons.IOType._
import org.openmole.ui.ide.workflow.implementation.PrototypeUI
import scala.collection.mutable.HashSet

trait IGenericTaskModelUI {
  def name: String
  
  def getType: Class[_]
  
  def addPrototype(p: PrototypeUI,ioType: IOType)
  
  def prototypesIn: HashSet[PrototypeUI]
  
  def prototypesOut: HashSet[PrototypeUI]
}
//public interface IGenericTaskModelUI<T extends IGenericTask> extends IObjectModelUI<T>{
//    public void proceed();
//    void addPrototype(PrototypeUI p,IOType ioType);
//    Set<PrototypeUI> getPrototypesIn();
//    Set<PrototypeUI> getPrototypesOut();
//    String getName();
//    Class getType();
//}var prototypesOut= HashSet.empty[PrototypeUI]