/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.ui.console.internal.command

import org.codehaus.groovy.tools.shell.CommandSupport
import org.codehaus.groovy.tools.shell.Shell
import org.openmole.misc.tools.service.HierarchicalRegistry
import org.openmole.core.model.execution.IEnvironment
import org.openmole.misc.workspace.Workspace
import org.openmole.ui.console.internal.command.initializer.EnvironmentInitializer
import org.openmole.ui.console.internal.command.initializer.IInitializer
import org.openmole.ui.console.internal.command.initializer.WorkspaceInitializer
import java.util.List
import scala.collection.JavaConversions._

class Init(shell: Shell, muteShell: Shell, string: String, string1: String) extends CommandSupport(shell, string, string1) {
  
  private val initializers = new HierarchicalRegistry[IInitializer] 
  initializers.register(Workspace.getClass, new WorkspaceInitializer)
  initializers.register(classOf[IEnvironment], new EnvironmentInitializer(muteShell))
  

  override def execute(list: List[_]): Object = {
    for (arg <- list) {
      val obj = shell.execute(arg.asInstanceOf[String])
      
      System.out.println(obj.getClass.getCanonicalName)
      
      if(obj.getClass == classOf[Class[_]]) {
        for (initializer <- initializers.closestRegistred(obj.asInstanceOf[Class[_]])) {
          initializer.initialize(null, obj.asInstanceOf[Class[_]])
        }
      } else {
        for (initializer <- initializers.closestRegistred(obj.getClass)) {
          initializer.initialize(obj, obj.getClass)
        }
      }

    }
    null
  }
}
