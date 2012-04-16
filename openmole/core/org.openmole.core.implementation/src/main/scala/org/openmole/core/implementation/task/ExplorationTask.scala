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

package org.openmole.core.implementation.task

import org.openmole.core.implementation.data._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.sampling.ISampling
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.task.IExplorationTask
import org.openmole.core.implementation.data.DataMode
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.exception.UserBadDataError
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer

object ExplorationTask {
  type SampledValues = Iterable[Iterable[IVariable[_]]]
 
  
  def apply(name: String, sampling: ISampling)(implicit plugins: IPluginSet) = {
    new TaskBuilder { builder =>
      
      def toTask = 
        new ExplorationTask(name, sampling) {
          val inputs = builder.inputs + sampling.inputs
          val outputs = builder.outputs ++ sampling.prototypes.map{p => new Data(p, DataMode(DataModeMask.explore)).toArray}
          val parameters = builder.parameters
        }
      
    }
  }
  
}

sealed abstract class ExplorationTask(val name: String, val sampling: ISampling)(implicit val plugins: IPluginSet) extends Task with IExplorationTask {
  
  //If input prototype as the same name as the output it is erased
  override protected def process(context: IContext) = {
    val sampled = sampling.build(context).toIterable

    val variablesValues = TreeMap.empty[IPrototype[_], ArrayBuffer[Any]] ++ sampling.prototypes.map{p => p -> new ArrayBuffer[Any](sampled.size)}
 
    for(sample <- sampled; v <- sample) variablesValues.get(v.prototype) match {
      case Some(b) => b += v.value
      case None =>
    }
    
    context ++ variablesValues.map{
      case(k,v) => 
        try new Variable(k.toArray.asInstanceOf[IPrototype[Array[_]]], 
                         v.toArray(k.`type`.asInstanceOf[Manifest[Any]]))
        catch {
          case e: ArrayStoreException => throw new UserBadDataError("Cannot fill factor values in " + k.toArray + ", values " + v)
        }
    }
  }
 
}
