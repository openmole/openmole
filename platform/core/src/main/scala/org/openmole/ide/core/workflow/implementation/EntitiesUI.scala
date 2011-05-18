/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.workflow.model.IEntityUI
import scala.collection.mutable.HashSet

object EntitiesUI {

  var entities = Map(Constants.TASK -> new HashSet[IEntityUI],
                     Constants.PROTOTYPE -> new HashSet[IEntityUI],
                     Constants.SAMPLING -> new HashSet[IEntityUI],
                     Constants.ENVIRONMENT -> new HashSet[IEntityUI])
  
  
  def getAll(entityType: String) = entities(entityType)
  
  def register(entity: IEntityUI) = entities(entity.entityType) += entity
  
  def remove(entity: IEntityUI) = entities(entity.entityType).remove(entity)
  
  def clearAll(entityType: String) = entities(entityType).clear
}