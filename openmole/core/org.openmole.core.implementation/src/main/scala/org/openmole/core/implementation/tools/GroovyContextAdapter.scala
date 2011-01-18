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

import groovy.lang.Binding
import java.io.File
import org.openmole.commons.tools.groovy.IGroovyProxy
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.internal.Activator._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.misc.workspace.IWorkspace

import scala.collection.JavaConversions.asJavaIterable

object GroovyContextAdapter{
  val globalContextVar = new Prototype[IContext]("global", classOf[IContext])
  val contextVar = new Prototype[IContext]("context", classOf[IContext])
  val workspaceVar = new Prototype[IWorkspace]("workspace", classOf[IWorkspace])
    
  def fromContextToBinding(global: IContext, context: IContext) = {
    val binding = new Binding

    binding.setVariable(globalContextVar.name, global)
    binding.setVariable(contextVar.name, context)
    binding.setVariable(workspaceVar.name, workspace)
    context.variables.values.foreach{in => binding.setVariable(in.prototype.name, in.value)}

    binding
  }
}

trait GroovyContextAdapter extends IGroovyProxy {
  
  //override def execute(binding: Binding): Object = groovyShellProxy.execute(binding)
  def execute(variables: Iterable[IVariable[_]]): Object =  {
    val binding = new Binding
    
    for(v <- variables) binding.setVariable(v.prototype.name, v.value)
    execute(binding)
  }
  
  def execute(global: IContext, binding: IContext): Object = execute(GroovyContextAdapter.fromContextToBinding(global, binding))
    
}
