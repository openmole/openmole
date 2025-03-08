/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.tools.service

import java.io.PrintStream
import org.apache.commons.exec.PumpStreamHandler
import org.apache.commons.exec.ShutdownHookProcessDestroyer

object ProcessUtil {
  val processDestroyer = new ShutdownHookProcessDestroyer

  def executeProcess(process: Process, out: PrintStream, err: PrintStream) = {
    val pump = new PumpStreamHandler(out, err)

    pump.setProcessOutputStream(process.getInputStream)
    pump.setProcessErrorStream(process.getErrorStream)

    processDestroyer.add(process)
    try {
      pump.start
      try process.waitFor
      catch {
        case e: Throwable =>
          def kill(p: ProcessHandle) = p.destroyForcibly()
          process.descendants().forEach(kill)
          kill(process.toHandle)

          throw e
      }
      finally {
        pump.stop
      }
    }
    finally processDestroyer.remove(process)
    process.exitValue
  }
}
