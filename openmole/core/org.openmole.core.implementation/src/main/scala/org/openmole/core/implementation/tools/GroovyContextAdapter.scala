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
import org.openmole.commons.tools.groovy.IGroovyProxy
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.misc.workspace.Workspace


object GroovyContextAdapter{
  val contextVar = new Prototype("context", classOf[IContext])
  val workspaceVar = new Prototype("workspace", Workspace.getClass)
  
  def fromContextToBinding(context: IContext) = {
    val binding = new Binding

    binding.setVariable(contextVar.name, context)
    binding.setVariable(workspaceVar.name, Workspace)
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
  
  def execute(binding: IContext): Object = execute(GroovyContextAdapter.fromContextToBinding(binding))
    
}
