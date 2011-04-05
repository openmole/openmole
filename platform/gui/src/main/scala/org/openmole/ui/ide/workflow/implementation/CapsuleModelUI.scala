/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI

class CapsuleModelUI(var taskModel: Option[IGenericTaskModelUI] = None, var nbInputSlots: Int = 1) extends ICapsuleModelUI{

  val category= "Task Tapsules"
  var startingCapsule= false
  var containsTask= false
  
  def this(taskModel: IGenericTaskModelUI)= this(Some(taskModel))
//  def this(tModel: IGenericTaskModelUI[IGenericTask], capsModule: CapsuleModelUI[T])= this(tModel, capsModule.nbInputSlots)
  
  def addInputSlot= nbInputSlots+= 1
  
  def removeInputSlot= nbInputSlots-= 1
  
  def setTaskModel(tModel: IGenericTaskModelUI)={
    taskModel= Some(tModel)
    containsTask= true
  }
  
  override def defineStartingCapsule(on: Boolean)= {
    startingCapsule= on
    if (on) nbInputSlots= 1
  }
}

//
//public class CapsuleModelUI<T extends IGenericCapsule> extends ObjectModelUI implements ICapsuleModelUI {
//
//    public static CapsuleModelUI EMPTY_CAPSULE_MODEL = new CapsuleModelUI();
//    private IGenericTaskModelUI<IGenericTask> taskModel;
//    private transient int nbInputSlots = 0;
//    private boolean startingCapsule = false;
//    private final static String category = "Task Tapsules";
//    private boolean containsTask = false;
//
//    CapsuleModelUI() {
//        this(TaskModelUI.EMPTY_TASK_MODEL);
//    }
//
//    CapsuleModelUI(IGenericTaskModelUI<IGenericTask> taskModel) {
//        this.taskModel = taskModel;
//    }
//
//    public boolean containsTask() {
//        return containsTask;
//    }
//
//    @Override
//    public IGenericTaskModelUI<IGenericTask> getTaskModel() {
//        return taskModel;
//    }
//
//    @Override
//    public void setTaskModel(IGenericTaskModelUI taskModel) {
//        this.taskModel = taskModel;
//        this.containsTask = true;
//    }
//
//    @Override
//    public String getCategory() {
//        return category;
//    }
//
//    @Override
//    public int getNbInputslots() {
//        return nbInputSlots;
//    }
//
//    @Override
//    public void addInputSlot() {
//        nbInputSlots++;
//    }
//
//    @Override
//    public boolean isSlotRemovable() {
//        return (nbInputSlots > 1 ? true : false);
//    }
//
//    @Override
//    public boolean isSlotAddable() {
//        return (nbInputSlots < ApplicationCustomize.NB_MAX_SLOTS ? true : false);
//    }
//
//    @Override
//    public void removeInputSlot() {
//        nbInputSlots -= 1;
//    }
//
//    @Override
//    public void eventOccured(Object t) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public void defineAsStartingCapsule() {
//        nbInputSlots = 1;
//        startingCapsule = true;
//    }
//
//    @Override
//    public void defineAsRegularCapsule() {
//        startingCapsule = false;
//    }
//
//    @Override
//    public boolean isStartingCapsule() {
//        return startingCapsule;
//    }
//
//    @Override
//    public void objectChanged(Object obj) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//}
