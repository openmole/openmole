/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.core.model.task.IGenericTask

class MoleTaskModelUI[T<: IGenericTask](taskUI: TaskUI) extends GenericTaskModelUI(taskUI) {

  override def proceed= new UnsupportedOperationException("proceed is not supported yet in MoleTaskModelUI.")
  
  override def eventOccured(t: Object)= new UnsupportedOperationException("eventOccured is not supported yet in MoleTaskModelUI.")
}

//<T extends IGenericTask> extends GenericTaskModelUI {
//
//    public MoleTaskModelUI(TaskUI taskUI) {
//        super(taskUI);
//    }
//
//    @Override
//    public void proceed() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public void eventOccured(Object t) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }