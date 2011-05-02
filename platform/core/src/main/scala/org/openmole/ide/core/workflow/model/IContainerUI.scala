/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.exception.MoleExceptionManagement
import scala.collection.mutable.HashMap

trait IContainerUI {
  def getAll= entities.values
  
  var entities= new HashMap[Tuple2[String,Class[_]],IEntityUI] 
  
  def getEntityTuple(entity: IEntityUI): Tuple2[String,Class[_]]= Tuple2[String,Class[_]](entity.name,entity.coreClass)
  
  def register(entity: IEntityUI)= entities+= getEntityTuple(entity)-> entity
  
  def removeEntity(entity: IEntityUI)= entities.remove(getEntityTuple(entity))
  
 // def get(st: String, entityType: Class[_]): IEntityUI= entities.getOrElse(Tuple2[String,Class[_]](st,entityType), new MoleExceptionManagement.showException("The entity " + st + " :: " + entityType.toString + " does not exist."))
  
  def contains(entity: IEntityUI): Boolean= entities.contains(getEntityTuple(entity))
  
  //override def getAll= entities.values
  
  def clearAll= entities.clear
}


//public interface IContainerUI {
//    void register(IEntityUI entity);
//    void removeEntity(IEntityUI entity) throws UserBadDataError;
//    IEntityUI get(String st,Class type) throws UserBadDataError;
//    Collection<IEntityUI> getAll();
//    void clearAll();
//    boolean contains(IEntityUI entity);
//}