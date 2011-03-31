/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

import org.openmole.misc.eventdispatcher.IObjectListener

trait IObjectModelUI[T] extends IObjectListener[T] {
  def objectChanged(obj: T)
  
  def updateData()
  
 // def getCategory(): String
}
//
//<T> extends IObjectListener<T>{
////public interface IObjectModelUI <T>{
//    void objectChanged(T obj);
//    void updateData();
//    String getModelGroup();
//    String getCategory();