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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.tools

import org.openmole.core.implementation.data.{Variable,Prototype}
import org.openmole.core.model.data.{IDataSet,IContext,IVariable,IPrototype}
import org.openmole.misc.exception.InternalProcessingError
import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.implementation.data.Context._

object ContextAggregator {
 
  def aggregate(aggregate: IDataSet, toArray: PartialFunction[String, Manifest[_]] , toAggregateList: Iterable[IVariable[_]]): IContext = {
    val toAggregate = toAggregateList.groupBy(_.prototype.name)
    
    aggregate.foldLeft(List.empty[IVariable[_]]) {
      case(acc, d) =>
        val merging = if(toAggregate.isDefinedAt(d.prototype.name)) toAggregate(d.prototype.name) else Iterable.empty
  
        if(toArray.isDefinedAt(d.prototype.name)) {
          val manifest = toArray(d.prototype.name)

          val array = manifest.newArray(merging.size)
          merging.zipWithIndex.foreach{e => java.lang.reflect.Array.set(array, e._2, e._1.value)}
          new Variable(new Prototype(d.prototype.name, manifest.arrayManifest).asInstanceOf[IPrototype[Any]], array) :: acc
        } else if(!merging.isEmpty) { 
          if(merging.size > 1) throw new InternalProcessingError("Variable " + d.prototype + " has been found multiple times, it doesn't match data flow specification.")        
          new Variable(d.prototype.asInstanceOf[IPrototype[Any]], merging.head.value) :: acc
        } else acc
    }.toContext
  }

}
