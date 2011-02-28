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

import org.openmole.core.implementation.data.{DataSet,Variable,Prototype, Context}
import org.openmole.core.model.data.{IDataSet,IData,IContext,IVariable,IPrototype}
import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.obj.ClassUtils._

object ContextAggregator {
 
  def aggregate(aggregate: IDataSet, toArrayFonc: PartialFunction[String, Manifest[_]] , toAggregateList: Iterable[IVariable[_]]): IContext = {
    val inContext = new Context   
    val toAggregate = toAggregateList.groupBy(_.prototype.name)
    
    for(d <- aggregate; if(toAggregate.isDefinedAt(d.prototype.name))) {
      if(toArrayFonc.isDefinedAt(d.prototype.name)) { 
        val merging = toAggregate(d.prototype.name)
        val manifest = toArrayFonc(d.prototype.name)
        
        val array = manifest.newArray(merging.size).asInstanceOf[Array[Any]]
        merging.map{_.value}.zipWithIndex.foreach{e => array(e._2) = e._1}
        
        inContext += new Variable(new Prototype(d.prototype.name, manifest.arrayManifest.erasure).asInstanceOf[IPrototype[Any]], array) 
      } else if (toAggregate(d.prototype.name).size > 1) {
        throw new InternalProcessingError("Variable " + d.prototype.name + " has been found multiple times before and it does'nt match data flow specification.")        
      } else inContext += new Variable(d.prototype.asInstanceOf[IPrototype[Any]], toAggregate(d.prototype.name).head.value)
    }  
    
    inContext
  }

}
