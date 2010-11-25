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

import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.tools.IContextBuffer
import org.openmole.core.model.tools.IVariableBuffer
import org.openmole.core.implementation.data.Prototype._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer
import org.openmole.commons.tools.service.ManifestUtil._

object ContextBuffer {
  
  def apply(context: IContext, toClone: Set[String]): ContextBuffer = {
    val ret = new ContextBuffer(false)
    ret ++= (context, toClone)
  }
  
  def apply(context: IContext, toClone: Set[String], varNames: Iterable[String]): ContextBuffer = {
    val ret = new ContextBuffer(false)
    ret ++= (context, toClone, varNames)
  }
}


class ContextBuffer(forceArray: Boolean) extends IContextBuffer {
  val variableBuffers = new ListBuffer[IVariableBuffer]
  
  override def toContext = {
    var variableByName = new TreeMap[String, ListBuffer[IVariable[_]]] 
    
    for(buff <- variableBuffers) {
      val variable = buff.toVariable
      variableByName.get(variable.prototype.name) match {
        case None => 
          val listBuff = ListBuffer[IVariable[_]](variable)
          variableByName += ((variable.prototype.name, listBuff))
        case Some(listBuff) => 
          listBuff += variable
      } 
    }
    
    val ret = new Context
    for(listBuff <- variableByName.values) {
      if(listBuff.size == 1 && !forceArray) {
        ret += listBuff.head
      } else {
        val m = intersect(listBuff.map{_.prototype.`type`}: _*)
        val array = m.newArray(listBuff.size).asInstanceOf[Array[Any]]
        var i = 0
        for(v <- listBuff.map{_.value}) {
          array(i) = v
          i += 1
        }
        ret += new Variable(toArray(listBuff.head.prototype).asInstanceOf[IPrototype[Array[Any]]], array)
      }
    }
    ret
  }
  
  override def += (v: IVariableBuffer): this.type = {
    variableBuffers += v
    this
  }
  
  override def ++= (context: IContext, toClone: Set[String], varNames: Iterable[String]): this.type = {
    for(name <- varNames) {
      val v = context.variable(name) match {
        case None =>
        case Some(v) =>
          if(toClone.contains(v.prototype.name)) this += new CloneVariableBuffer(v)
          else this += new VariableBuffer(v)
      }

    }
    this
  }
  
  override def ++= (context: IContext, toClone: Set[String]): this.type = {
    for(v <- context) {
      if(toClone.contains(v.prototype.name)) this += new CloneVariableBuffer(v)
      else this += new VariableBuffer(v)
    }
    this
  }
}
