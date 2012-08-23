/*
 * Copyright (C) 2010 Romain Reuillon
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
import org.openmole.misc.tools.script.GroovyProxyPool
import org.openmole.core.implementation.data.{ Prototype, Variable, Context }
import org.openmole.core.implementation.tools.GroovyContextAdapter._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IPrototype

class ContextToGroovyCode(source: String, libs: Iterable[File]) {

  @transient lazy val editorPool = new GroovyProxyPool(source, libs)

  def execute(binding: Binding): Object = editorPool.execute(binding)

  def execute(context: IContext): Object = editorPool.execute(context.toBinding)

  def execute(context: IContext, output: IDataSet): IContext = {
    val binding = context.toBinding
    execute(binding)
    fetchVariables(context, output, binding)
  }

  def fetchVariables(context: IContext, output: IDataSet, binding: Binding): IContext = {
    val variables = binding.getVariables
    Context.empty ++ output.flatMap {
      data ⇒
        val out = data.prototype

        variables.get(out.name) match {
          case null ⇒ None
          case value ⇒
            if (out.accepts(value)) Some(new Variable(out.asInstanceOf[IPrototype[Any]], value))
            else throw new InternalProcessingError("Variable " + out.name + " of type " + value.asInstanceOf[AnyRef].getClass.getName + " has been found at the end of the execution of the groovy code but type doesn't match : " + out.`type`.erasure.getName + ".")
        }
    }
  }

}
