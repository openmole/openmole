/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.misc.exception.UserBadDataError
import org.openmole.ui.ide.workflow.model.IContainerUI
import org.openmole.ui.ide.workflow.model.IEntityUI
import scala.collection.mutable.HashMap

class ContainerUI extends IContainerUI{
  var entities= new HashMap[Tuple2[String,Class[_]],IEntityUI] 
  
  def getEntityTuple(entity: IEntityUI): Tuple2[String,Class[_]]= Tuple2[String,Class[_]](entity.name,entity.entityType)
  
  override def register(entity: IEntityUI)= entities+= getEntityTuple(entity)-> entity
  
  override def removeEntity(entity: IEntityUI)= entities.remove(getEntityTuple(entity))
  
  override def get(st: String, entityType: Class[_]): IEntityUI= {
    entities.getOrElse(Tuple2[String,Class[_]](st,entityType),throw new UserBadDataError("The entity " + st + " :: " + entityType.toString + " doest not exist."))
  }
  
  override def contains(entity: IEntityUI): Boolean= entities.contains(getEntityTuple(entity))
  
  override def getAll: Collection[IEntityUI]= entities.values
  
  override def clearAll {entities.clear}
}

//public class ContainerUI implements IContainerUI {
//
//    private Map<Tuple2<String, Class>, IEntityUI> entities = new HashMap<Tuple2<String, Class>, IEntityUI>();
//
//    @Override
//    public void register(IEntityUI entity) {
//        this.entities.put(new Tuple2<String, Class>(entity.getName(), entity.getType()), entity);
//    }
//
//    @Override
//    public void removeEntity(IEntityUI entity) throws UserBadDataError {
//        this.entities.remove(new Tuple2<String, Class>(entity.getName(), entity.getType()));
//    }
//
//    @Override
//    public IEntityUI get(String st, Class type) throws UserBadDataError {
//        Tuple2 tuple = new Tuple2<String, Class>(st, type);
//        if (this.entities.containsKey(tuple)) {
//            return this.entities.get(tuple);
//        } else {
//            throw new UserBadDataError("The entity " + st + " :: " + type + " doest not exist.");
//        }
//    }
//
//    @Override
//    public boolean contains(IEntityUI entity){
//        return this.entities.containsKey(new Tuple2<String, Class>(entity.getName(), entity.getType()));
//    }
//    
//    @Override
//    public Collection<IEntityUI> getAll() {
//        return entities.values();
//    }
//
//    @Override
//    public void clearAll() {
//        entities.clear();
//    }
//}