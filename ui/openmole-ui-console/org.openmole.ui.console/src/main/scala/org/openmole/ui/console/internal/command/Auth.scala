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

import org.codehaus.groovy.tools.shell.CommandSupport
import org.codehaus.groovy.tools.shell.Shell
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.environment.AuthenticationMethod
import org.openmole.core.batch.environment.AuthenticationMethod._
import java.util.List
import scala.collection.JavaConversions._

class Auth(shell: Shell, muteShell: Shell, string: String, string1: String) extends CommandSupport(shell, string, string1) {

  override def execute(list: List[_]): Object = {
    if(list.head == "-l") {
      val method = shell.execute(list.tail.asInstanceOf[String]).asInstanceOf[Class[_]]
      Workspace.persistentList(method).foreach {
        case (i, m) => println(i + ": " + m)
      }
      null
    } else {
      val index = shell.execute(list.head.asInstanceOf[String]).asInstanceOf[Int]
      val obj = shell.execute(list.tail.asInstanceOf[String]).asInstanceOf[AuthenticationMethod]
      Workspace.instance.register(index, obj)
      null
    }
  }
  
  
}
