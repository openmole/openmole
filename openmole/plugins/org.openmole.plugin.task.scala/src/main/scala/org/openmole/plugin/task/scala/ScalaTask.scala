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

package org.openmole.plugin.task.scala

import java.io.File
import java.io.PrintWriter
import java.util.logging.Logger
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.apache.clerezza.scala.scripting.InterpreterFactory
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ListBuffer

class ScalaTask(val name: String, val code: String) extends Task {
  
  var libs = new TreeSet[File]
  var imports = new ListBuffer[String]
    
  def addImport(imp: String): this.type = {imports += imp; this}

  @transient lazy val factory = new InterpreterFactory
  
  override def process(context: IContext) = {
    val interpreter = factory.createInterpreter(new PrintWriter(System.out))
    context.values.foreach{v => interpreter.bind(v.prototype.name, v.value)}
    interpreter.addImports(imports.toSeq: _*)
    interpreter.interpret(code)
    Context.empty ++ outputs.map{o => new Variable(o.prototype.asInstanceOf[IPrototype[Any]], interpreter.valueOfTerm(o.prototype.name))}
  }
}

