/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.core.model.task.IGenericTask

class GroovyTaskModelUI[T<: IGenericTask](taskUI: TaskUI) extends GenericTaskModelUI(taskUI) {

  override def proceed= new UnsupportedOperationException("proceed is not supported yet in GroovyTaskModelUI.")
  
  override def eventOccured(t: Object)= new UnsupportedOperationException("eventOccured is not supported yet in GroovyTaskModelUI.")
}