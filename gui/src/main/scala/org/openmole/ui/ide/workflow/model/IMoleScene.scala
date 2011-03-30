/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

trait IMoleScene {
  def setLayout
  
  def refresh
  
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