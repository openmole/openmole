/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.core.model.task.IGenericTask

class TaskModelUI[T<: IGenericTask](taskUI: TaskUI) extends GenericTaskModelUI[T](taskUI){

  override def proceed= throw new UnsupportedOperationException("proceed in TaskModelUI is not supported yet.")
}
//
//public class TaskModelUI<T extends IGenericTask> extends GenericTaskModelUI{
//
//    public static IGenericTaskModelUI<IGenericTask>  EMPTY_TASK_MODEL = new TaskModelUI(new TaskUI());
//
//    public TaskModelUI(TaskUI taskUI) {
//        super(taskUI);
//    }
//
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
//}