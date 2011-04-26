/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.workflow.implementation.TaskUI

trait ICapsuleModelUI {
  def startingCapsule: Boolean
  
  def containsTask: Boolean

  def taskUI: Option[TaskUI]
  
  def setTaskUI(taskUI: TaskUI)
  
  def defineStartingCapsule(on: Boolean)
  
  def addInputSlot
  
  def nbInputSlots: Int
  
  def removeInputSlot
}


//ICapsuleModelUI<T extends IGenericCapsule> extends IObjectModelUI<T>{
//  boolean containsTask();
//  IGenericTaskModelUI<IGenericTask> getTaskModel();
//  void setTaskModel(IGenericTaskModelUI<IGenericTask> taskModel);
//  void defineAsStartingCapsule();
//  void defineAsRegularCapsule();
//  boolean isStartingCapsule();
//  void addInputSlot();
//  int getNbInputslots();
//  boolean isSlotRemovable();
//  boolean isSlotAddable();
//  void removeInputSlot();