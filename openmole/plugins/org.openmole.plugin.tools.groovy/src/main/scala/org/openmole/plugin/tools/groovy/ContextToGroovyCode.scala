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
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.tools.GroovyContextAdapter._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.execution.IProgress
import org.openmole.plugin.tools.code.IContextToCode


class ContextToGroovyCode(source: String, libs: Iterable[File]) extends IContextToCode {

  @transient lazy val editorPool = new GroovyProxyPool(source, libs)

  def execute(context: IContext, tmpVariables: Iterable[IVariable[_]]): Object = {
    execute(context, tmpVariables, IProgress.Dummy, Iterable.empty)
  }

  override def execute(context: IContext, tmpVariables: Iterable[IVariable[_]], progress: IProgress, output: Iterable[IData[_]]): Object = {
    val binding = context.toBinding

    for(variable <- tmpVariables) binding.setVariable(variable.prototype.name, variable.value)

    binding.setVariable(IContextToCode.progressVar.name, progress)
            
    val ret = editorPool.execute(binding)

    fetchVariables(context, output, binding)
    ret
  }

  def fetchVariables(context: IContext, output: Iterable[IData[_]], binding: Binding) = {
    val variables = binding.getVariables
    for (data <- output) {
      val out = data.prototype

      variables.get(out.name) match {
        case null =>
        case value =>
          if (out.accepts(value)) context += (out.asInstanceOf[IPrototype[Any]], value)
          else throw new InternalProcessingError("Variable " + out.name + " of type " + value.asInstanceOf[AnyRef].getClass.getName + " has been found at the end of the execution of the groovy code but type doesn't match : " + out.`type`.erasure.getName + ".")
      }
    }
  }

}
