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

package org.openmole.commons.tools.groovy

import groovy.lang.Binding
import groovy.lang.GroovyShell
import groovy.lang.Script
import java.io.File
import org.codehaus.groovy.runtime.InvokerHelper
import org.openmole.commons.exception.{InternalProcessingError, UserBadDataError}

class GroovyProxy(code: String, jars: Iterable[File]) extends IGroovyProxy {

  @transient 
  private lazy val compiledScript = {
    val groovyShell = new GroovyShell
    for(jar <- jars) {
      groovyShell.getClassLoader().addURL(jar.toURI().toURL());
    }
    try {
      groovyShell.parse("package script\n" + code)
    } catch {
      case t => throw new UserBadDataError("Script compilation error !\n The script was :\n" + code + "\n Error message was:" + t.getMessage);
    }
  }


  /**
   * This method run your compiled script.
   * @return the result of your script if a variable is returned.
   * @throws InternalProcessingError 
   */
  def execute(binding: Binding): Object = {
    compiledScript.setBinding(binding)
    val ret = compiledScript.run
    InvokerHelper.removeClass(compiledScript.getClass)
    compiledScript.setBinding(null)
    ret
  }
}
