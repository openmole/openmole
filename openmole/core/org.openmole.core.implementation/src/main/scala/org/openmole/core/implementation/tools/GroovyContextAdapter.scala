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

import groovy.lang.Binding
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IContext
import org.openmole.misc.tools.script.GroovyProxy
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.obj.ClassUtils._

object GroovyContextAdapter {
  val workspaceVar = new Prototype[Workspace]("workspace")
  
  implicit def contextDecorator(variables: IContext) = new {
    def toBinding = {
      val binding = new Binding
      binding.setVariable(workspaceVar.name, Workspace)
      
      //val seed = Workspace.newSeed
      //val rng = Workspace.newRNG(seed)
      
      //binding.setVariable(rngVar.name, rng)
      //binding.setVariable(seedVar.name, seed)
      variables.values.foreach{v => binding.setVariable(v.prototype.name, v.value)}
      binding
    }
  }
}

trait GroovyContextAdapter extends GroovyProxy { 
  import GroovyContextAdapter._
  def execute(variables: IContext): Object = execute(variables.toBinding)
}  
