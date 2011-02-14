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
import org.openmole.core.implementation.data.{DataSet,Variable,Prototype, Context}
import org.openmole.core.model.data.{IDataSet,IData,IContext,IVariable,IPrototype}
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import org.openmole.commons.tools.service.ManifestUtil._

object ContextAggregator {

 /* def dataIn1WhichAreAlsoIn2(one: IDataSet, two: IDataSet): IDataSet = {
    new DataSet(for (data <- one ; if two.contains(data.prototype.name)) yield data)
  }*/

  def aggregate(aggregate: IDataSet, forceArrays: Boolean, toAgregate: Iterable[IContext]): IContext = {
    val inContext = new Context
    
    var mergingVars = new TreeMap[String, ListBuffer[IVariable[_]]]

    for (current <- toAgregate) {
      for (data <- aggregate) {
        current.variable(data.prototype) match {
          case None =>
          case Some(tmp) =>  
            mergingVars.get(data.prototype.name) match {
              case None => mergingVars += data.prototype.name -> ListBuffer(tmp)
              case Some(buff) => buff += tmp
            }
        }
      }
    }

    for(merged <- mergingVars) {
      if(forceArrays || merged._2.size > 1) {  
        val m = intersect(merged._2.map{_.prototype.`type`}: _*)
        val array = m.newArray(merged._2.size).asInstanceOf[Array[Any]]
        merged._2.map{_.value}.zipWithIndex.foreach{e => array(e._2) = e._1}
        
        inContext += new Variable(Prototype.toArray(aggregate(merged._1).get.prototype).asInstanceOf[IPrototype[Any]], array) 
      }
      else inContext += new Variable(aggregate(merged._1).get.prototype.asInstanceOf[IPrototype[Any]], merged._2.head.value)
    }  
    
    inContext
  }

}
