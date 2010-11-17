/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
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

package org.openmole.plugin.task.systemexec

import org.openmole.plugin.tools.utils.ProcessUtils._
import org.openmole.commons.tools.io.StringBuilderOutputStream
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IContext
import java.io.PrintStream
import java.lang.StringBuilder
import java.lang.Integer

class SystemExecToStringTask(name: String, 
                             cmd: String, 
                             returnValue: Prototype[Integer], 
                             relativeDir: String,
                             val outString: Prototype[String] = null,
                             val errString: Prototype[String] = null) extends AbstractSystemExecTask(name,cmd,returnValue,relativeDir) {
  
    
  override protected def execute(process: Process, context: IContext):Integer = {    
    val outStringBuilder = new StringBuilder
    val errStringBuilder = new StringBuilder

    val ret = executeProcess(process,new PrintStream(new StringBuilderOutputStream(outStringBuilder)),new PrintStream(new StringBuilderOutputStream(errStringBuilder)))
    if(outString != null) context += (outString,outStringBuilder.toString)
    if(errString != null) context += (errString,errStringBuilder.toString)
    return ret
  }
}
