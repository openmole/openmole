/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.ui.console.internal.command

import org.codehaus.groovy.tools.shell.Shell
import org.openmole.misc.tools.service.HierarchicalRegistry
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ui.console.internal.command.viewer.BatchEnvironmentViewer
import org.openmole.ui.console.internal.command.viewer.EnvironmentViewer
import org.openmole.ui.console.internal.command.viewer.IViewer
import org.openmole.ui.console.internal.command.viewer.MoleExecutionViewer
import java.util.List
import scala.collection.JavaConversions._

class Print(shell: Shell, string: String, string1: String) extends UICommand(shell, string, string1) {
  private val viewers = new HierarchicalRegistry[IViewer]

  viewers.register(classOf[IEnvironment], new EnvironmentViewer)
  viewers.register(classOf[BatchEnvironment], new BatchEnvironmentViewer)
  viewers.register(classOf[IMoleExecution], new MoleExecutionViewer)
    

  override def execute(list: List[_]): Object = {

    if (list.isEmpty) return null
        
    val args = (shell.execute(list.head.asInstanceOf[String]), list.asInstanceOf[List[String]].tail.toArray)

    for (viewer <- viewers.closestRegistred(args._1.getClass)) viewer.view(args._1, args._2)
    
    null
  }
}
