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

package org.openmole.ui.console

import groovy.lang.Binding
import java.io.InputStream
import java.io.OutputStream
import org.codehaus.groovy.ant.Groovy
import org.codehaus.groovy.tools.shell.Command
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.IO
import org.openmole.core.structuregenerator.StructureGenerator
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace


object Console {

  val pluginManager = "plugin"
  val structureGenerator = "structure"
  val workspace = "workspace"
  val registry = "registry"
  
  val binding = new Binding
  val groovysh = new Groovysh(classOf[Groovy].getClassLoader, binding, new IO())

  val muteGroovysh = new Groovysh(classOf[Groovy].getClassLoader, binding, new IO(new InputStream {
        override def read: Int =  0
      }, new OutputStream {
        override def write(b: Int) = {}
      }, new OutputStream {
        override def write(b: Int) = {}
      }))
  
  setVariable(pluginManager, PluginManager)
  setVariable(structureGenerator, StructureGenerator.instance)
  setVariable(workspace, Workspace)

  def setVariable(name: String, value: Object) = binding.setVariable(name, value)

  def run(command: String) = groovysh.run(command)

  def leftShift(cmnd: Command): Object = groovysh.leftShift(cmnd)
}
