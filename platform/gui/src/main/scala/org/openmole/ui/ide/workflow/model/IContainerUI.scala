/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

import org.openmole.misc.exception.UserBadDataError
import scala.collection.mutable.HashMap

trait IContainerUI {
//  def register(entityUI: IEntityUI)
//
//  def removeEntity(entityUI: IEntityUI)
//  
//  def get(st: String, entityType: Class[_]): IEntityUI
//  
  def getAll: Set[IEntityUI]
//  
//  def clearAll
//  
//  def contains(entityUI: IEntityUI)
  
  var entities= new HashMap[Tuple2[String,Class[_]],IEntityUI] 
  
  def getEntityTuple(entity: IEntityUI): Tuple2[String,Class[_]]= Tuple2[String,Class[_]](entity.name,entity.entityType)
  
  override def register(entity: IEntityUI)= entities+= getEntityTuple(entity)-> entity
  
  override def removeEntity(entity: IEntityUI)= entities.remove(getEntityTuple(entity))
  
  override def get(st: String, entityType: Class[_]): IEntityUI= {
    entities.getOrElse(Tuple2[String,Class[_]](st,entityType),throw new UserBadDataError("The entity " + st + " :: " + entityType.toString + " doest not exist."))
  }
  
  override def contains(entity: IEntityUI): Boolean= entities.contains(getEntityTuple(entity))
  
  //override def getAll= entities.values
  
  override def clearAll {entities.clear}
}


//public interface IContainerUI {
//    void register(IEntityUI entity);
//    void removeEntity(IEntityUI entity) throws UserBadDataError;
//    IEntityUI get(String st,Class type) throws UserBadDataError;
//    Collection<IEntityUI> getAll();
//    void clearAll();
//    boolean contains(IEntityUI entity);
//}