/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.ui.ide.workflow.model.IObjectModelUI

class ObjectModelUI[T] extends IObjectModelUI[T]{

  val modelGroup: String
    
  override def objectChanged= throw new UnsupportedOperationException("objectChanged not supported yet.")
   
  override def updateData= throw new UnsupportedOperationException("updateData not supported yet.")
}

//
//    private String modelGroup;
//
//    @Override
//    public void objectChanged(Object obj) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public void updateData() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public String getModelGroup() {
//        return modelGroup;
//    }