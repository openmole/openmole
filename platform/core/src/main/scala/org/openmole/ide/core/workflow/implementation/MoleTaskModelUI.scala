/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

class MoleTaskModelUI(taskUI: TaskUI) extends GenericTaskModelUI(taskUI) {}

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