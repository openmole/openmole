/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.tools

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.data.{DataSet,Variable,Prototype}
import org.openmole.core.model.data.{IDataSet,IData,IContext,IVariable,IPrototype}
import scala.collection.immutable.TreeSet
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions
import scala.collection.JavaConversions._

object ContextAggregator {

  def dataIn1WhichAreAlsoIn2(one: IDataSet, two: IDataSet): IDataSet = {
    new DataSet(for (data <- one ; if two.contains(data.prototype.name)) yield data)
  }

  def aggregate(inContext: IContext, aggregate: IDataSet, toClone: Set[String], forceArrays: Boolean, toAgregate: Iterable[IContext]) = {
    var mergingVars = new TreeSet[String]

    for (current <- toAgregate) {
      for (data <- aggregate) {
        val inProt = data.prototype

        def getAndCloneIfNecessary[T](prot: IPrototype[T]): IVariable[T] = {
          val variable = current.variable(prot) match {
            case None => throw new InternalProcessingError("BUG Prototype not found in context.")
            case Some(v) => v
          }
          if(toClone.contains(prot.name)) ClonningService.clone(variable) else variable
        }
        
     
        if ((!data.mode.isOptional || data.mode.isOptional) && current.containsVariableWithName(inProt.name)) {
          inContext.+=(
            if(forceArrays || inContext.containsVariableWithName(inProt.name)) {
              val inProtArray = Prototype.toArray(inProt)

              if (mergingVars.contains(inProt.name)) {
                val curArray = inContext.value(inProtArray) match {
                  case None => throw new InternalProcessingError("BUG should allways exist.")
                  case Some(v) => v
                }
                
                val newVal = curArray ++ Vector(getAndCloneIfNecessary(inProt).value)
                new Variable(inProtArray, JavaConversions.asJavaIterable(newVal))
              } else {
                val tmp = getAndCloneIfNecessary(inProt)  
                val agregationList = 
                  if (inContext.containsVariableWithName(inProt.name)) 
                    Vector(tmp.value) ++ (inContext.value(inProt.name) match {
                      case None => throw new InternalProcessingError("BUG should allways exist.")
                      case Some(v) => v
                    })
                else Vector(tmp.value)
                new Variable(inProtArray, JavaConversions.asJavaIterable(agregationList))
              }
            } else getAndCloneIfNecessary(inProt))
        }
      }
    }
  }
  
  /*def typed[T](inContext: IContext, data: IData[T],  current: IContext, toClone: Set[String], forceArrays: Boolean) = {
    val inProt = data.prototype
    var mergingVars = new TreeSet[String]

    def getAndCloneIfNecessary[T](prot: IPrototype[T]): IVariable[T] = {
          val variable = current.variable(prot) match {
            case None => throw new InternalProcessingError("BUG Prototype not found in context.")
            case Some(v) => v
          }
          if(toClone.contains(prot.name)) ClonningService.clone(variable) else variable
        }
        
     
        if ((!data.mode.isOptional || data.mode.isOptional) && current.containsVariableWithName(inProt.name)) {
          inContext.+=(
            if(forceArrays || inContext.containsVariableWithName(inProt.name)) {
              val inProtArray: IPrototype[java.lang.Iterable[T]] = Prototype.toArray[T](inProt)

              if (mergingVars.contains(inProt.name)) {
                var curArray: Iterable[T] = inContext.value(inProtArray) match {
                  case None => throw new InternalProcessingError("BUG should allways exist.")
                  case Some(v) => v.toList ++ List(getAndCloneIfNecessary(inProt).value)
                }
                
             val newVal = curArray add getAndCloneIfNecessary(inProt).value
                //FIXME?downcasting
                new Variable(inProtArray, JavaConversions.asJavaIterable(curArray))
              } else {
                val tmp = getAndCloneIfNecessary(inProt)  
                val agregationList = 
                  if (inContext.containsVariableWithName(inProt.name)) 
                    Vector(tmp.value) ++ (inContext.value(inProt.name) match {
                      case None => throw new InternalProcessingError("BUG should allways exist.")
                      case Some(v) => v
                    })
                else Vector(tmp.value)
                //FIXME?downcasting
                new Variable(inProtArray.asInstanceOf[IPrototype[Iterable[Any]]], agregationList)
              }
            } else getAndCloneIfNecessary(inProt))
        }
  }*/

}
