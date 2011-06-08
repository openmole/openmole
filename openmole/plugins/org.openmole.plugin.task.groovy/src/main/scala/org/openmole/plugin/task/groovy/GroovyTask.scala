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

package org.openmole.plugin.task.groovy

import java.io.File
import java.util.logging.Logger
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.openmole.plugin.task.code.CodeTask
import org.openmole.plugin.tools.code.{ISourceCode,StringSourceCode,FileSourceCode}
import org.openmole.plugin.tools.groovy.ContextToGroovyCode
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.pluginmanager.PluginManagerInfo
import org.openmole.core.model.data.IContext
import org.openmole.core.model.execution.IProgress
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ListBuffer

class GroovyTask(name: String, private var _code: ISourceCode) extends CodeTask(name) {
  
  def this(name: String) = this(name, new StringSourceCode(""))
  def this(name: String, code: String) = this(name, new StringSourceCode(code))
  def this(name: String, code: File) = this(name, new FileSourceCode(code))
  
  var plugins = new TreeSet[File]
  var libs = new TreeSet[File]
  var imports = new ListBuffer[String]
  
  @transient override lazy val contextToCode = new ContextToGroovyCode(code, libs)
  
  private def code: String = {
    if(!imports.isEmpty) imports.map( "import " + _ ).reduceLeft( (l, r) => l + '\n' + r) + '\n' + _code.code
    else _code.code
  }
  
  override def process(context: IContext, progress: IProgress) = {
      if(PluginManagerInfo.enabled) PluginManager.loadIfNotAlreadyLoaded(plugins) 
      else if(!plugins.isEmpty) Logger.getLogger(classOf[GroovyTask].getName).warning("Plugin haven't been loadded cause application isn't runned in an osgi environment.")
      super.process(context, progress)
  }
  
  def addImport(imp: String): this.type = {imports += imp; this}
  def addLib(lib: File): this.type = {libs += lib; this}
  def addLib(lib: String): this.type = addLib(new File(lib))
  def setCode(code: String): this.type = {_code = new StringSourceCode(code); this}
  def setCodeFile(file: File): this.type = {_code = new FileSourceCode(file); this}
  def setCodeFile(file: String): this.type = setCodeFile(new File(file))
  def addPlugin(plugin: File): this.type = {plugins += plugin; this}
  def addPlugin(plugin: String): this.type = addPlugin(new File(plugin))
}
