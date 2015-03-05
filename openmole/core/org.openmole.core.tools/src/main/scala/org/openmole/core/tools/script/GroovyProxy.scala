/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.tools.script

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.io.Prettifier

import scala.collection.JavaConversions._
import scala.collection.JavaConversions
import groovy.lang.Binding
import groovy.lang.GroovyShell
import java.io.File
import java.net.URLClassLoader
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.runtime.InvokerHelper
import collection.JavaConversions._
import Prettifier._

object GroovyProxy {

  def apply(code: String, jars: Iterable[File] = Iterable.empty) = new GroovyProxy(code, jars)

}

class GroovyProxy(code: String, jars: Iterable[File] = Iterable.empty) extends GroovyFunction {

  @transient
  private lazy val compiledScript = {
    //val classLoader = new URLClassLoader(jars.map { jar ⇒ jar.getAbsoluteFile.toURI.toURL }.toArray, classOf[GroovyShell].getClassLoader)
    val config = new CompilerConfiguration
    /* Add optimisations when indy version of groovy will be used
    config.getOptimizationOptions.put("indy", true)
    config.getOptimizationOptions.put("int", false) */
    config.setClasspathList(jars.map { _.getAbsolutePath }.toList)
    val groovyShell = new GroovyShell(config)
    try groovyShell.parse("package script\n" + code)
    catch {
      case t: Throwable ⇒
        throw new UserBadDataError("Script compilation error !\n The script was :\n" + code + "\n Error (" + t.getClass.getName + ") message was:" + t.getMessage);
    }
  }

  def apply(binding: Binding) = execute(binding)

  /**
   * This method run your compiled script.
   * @return the result of your script if a variable is returned.
   * @throws InternalProcessingError
   */
  def execute(binding: Binding = new Binding) = compiledScript.synchronized {
    executeUnsynchronized(binding)
  }

  def executeUnsynchronized(binding: Binding = new Binding) = try {
    compiledScript.setBinding(binding)
    val ret = compiledScript.run
    InvokerHelper.removeClass(compiledScript.getClass)
    compiledScript.setBinding(null)
    ret
  }
  catch {
    case t: Throwable ⇒
      throw new UserBadDataError(
        s"""Script execution error !
          |The script was:
          |${code}
          |It has raised the exception:
          |""".stripMargin + t.stackStringWithMargin
      )
  }

}
