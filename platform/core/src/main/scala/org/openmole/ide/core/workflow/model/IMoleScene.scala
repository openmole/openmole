/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.workflow.implementation.MoleSceneManager

trait IMoleScene {
  def manager: MoleSceneManager
  
  def setLayout
  
  def refresh
  
  def validate
  
  def initCapsuleAdd(w: ICapsuleView)
}
//IMoleScene{
//
//    void setLayout();
//   // IConnectable createTaskCapsule();
//   // TaskViewUI createTask(IGenericTask obj);
//    void refresh();
//    //void setMovable(boolean b);
//    MoleSceneManager getManager();
//    void initCapsuleAdd(ICapsuleView w);