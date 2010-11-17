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

package org.openmole.plugin.task.groovy

import java.io.File
import org.openmole.commons.tools.io.FileUtil.fileOrdering
import org.openmole.plugin.task.code.CodeTask
import org.openmole.plugin.tools.code.{ISourceCode,StringSourceCode,FileSourceCode}
import org.openmole.plugin.tools.groovy.ContextToGroovyCode
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ListBuffer

class GroovyTask(name: String) extends CodeTask(name) {
  var libs = new TreeSet[File]
  var imports = new ListBuffer[String]
  var _code: ISourceCode = new StringSourceCode("")
  
  @transient override lazy val contextToCode = new ContextToGroovyCode(_code.code, libs)
  
  private def code: String = {
    imports.map( "import " + _ ).reduceLeft( (l, r) => l + '\n' + r) + '\n' + _code.code
  }
  
  def addImport(imp: String): this.type = {imports += imp; this}
  def addLib(lib: File): this.type = {libs += lib; this}
  def addLib(lib: String): this.type = {addLib(new File(lib)); this}
  def setCode(code: String): this.type = {_code = new StringSourceCode(code); this}
  def setCodeFile(file: File): this.type = {_code = new FileSourceCode(file); this}
  def setCodeFile(file: String): this.type = {setCodeFile(new File(file)); this}
  
}
