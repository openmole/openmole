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
import java.util.Random
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IVariable
import org.openmole.misc.tools.groovy.GroovyProxy
import org.openmole.misc.tools.service.RNG
import org.openmole.misc.workspace.Workspace


object GroovyContextAdapter {
  val workspaceVar = new Prototype("workspace", Workspace.getClass)
  val rngVar = new Prototype("rng", classOf[Random])
  
  implicit def variablesDecorator(variables: Iterable[IVariable[_]]) = new {
    def toBinding = {
      val binding = new Binding
      binding.setVariable(workspaceVar.name, Workspace)
      binding.setVariable(rngVar.name, RNG.rng)
      variables.foreach{v => binding.setVariable(v.prototype.name, v.value)}
      binding
    }
  }
}

trait GroovyContextAdapter extends GroovyProxy { 
  import GroovyContextAdapter._
  def execute(variables: Iterable[IVariable[_]]): Object = execute(variables.toBinding)
}  
