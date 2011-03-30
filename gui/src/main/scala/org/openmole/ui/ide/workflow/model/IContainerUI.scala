/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

trait IContainerUI {
  def register(entityUI: IEntityUI)

  def removeEntity(entityUI: IEntityUI)
  
  def get(st: String, entityType: Class[_]): IEntityUI
  
  def getAll: Collection[IEntityUI]
  
  def clearAll
  
  def contains(entityUI: IEntityUI)
}


//public interface IContainerUI {
//    void register(IEntityUI entity);
//    void removeEntity(IEntityUI entity) throws UserBadDataError;
//    IEntityUI get(String st,Class type) throws UserBadDataError;
//    Collection<IEntityUI> getAll();
//    void clearAll();
//    boolean contains(IEntityUI entity);
//}