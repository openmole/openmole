/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

trait ICapsuleModelUI {
  def startingCapsule: Boolean
  
  def containsTask: Boolean

  def taskModel: Option[IGenericTaskModelUI]
  
  def setTaskModel(taskModel: IGenericTaskModelUI)
  
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