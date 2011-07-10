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

package org.openmole.plugin.tools.groovy

import groovy.lang.Binding
import java.io.File
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.groovy.GroovyProxyPool
import org.openmole.core.implementation.data.{Prototype, Variable}
import org.openmole.core.implementation.tools.GroovyContextAdapter._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.plugin.tools.code.IContextToCode


class ContextToGroovyCode(source: String, libs: Iterable[File]) extends IContextToCode {

  @transient lazy val editorPool = new GroovyProxyPool(source, libs)
  
  def execute(binding: Binding): Object = editorPool.execute(binding)
  
  def execute(context: IContext): Object =  editorPool.execute(context.toBinding)
 
  override def execute(context: IContext, output: Iterable[IData[_]]): Iterable[IVariable[_]] = {
    val binding = context.toBinding
    execute(binding)
    fetchVariables(context, output, binding)
  }

  def fetchVariables(context: IContext, output: Iterable[IData[_]], binding: Binding): Iterable[IVariable[_]] =  {
    val variables = binding.getVariables
    output.flatMap {
      data =>
      val out = data.prototype

      variables.get(out.name) match {
        case null => Option.empty[IVariable[_]]
        case value =>
          if (out.accepts(value)) Some(new Variable(out.asInstanceOf[IPrototype[Any]], value))
          else throw new InternalProcessingError("Variable " + out.name + " of type " + value.asInstanceOf[AnyRef].getClass.getName + " has been found at the end of the execution of the groovy code but type doesn't match : " + out.`type`.erasure.getName + ".")
      }
    }
  }

}
