/*
 *  Copyright (C) 2015 Jonathan Passerat-Palmbach
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.care

import org.openmole.tool.file._

import org.openmole.tool.logger.Logger
import org.openmole.plugin.task.systemexec

object CARETask extends Logger {

  /**
   * Systemexec task execute an external process.
   * To communicate with the dataflow the result should be either a file / category or the return
   * value of the process.
   */
  def apply(archiveLocation: String, command: String) = {

    val archive = archiveLocation.split('/').last

    val extract = s"./${archive} -x"
    val reExecute = s"./${archive}/re-execute.sh"
    val workDirectory = s"grep '-w' ${archive}/re-execute.sh" // | cut \\-d \' \' \\-f 2 | tr \\-d \\'"
    //    val linkInOutputs =
    //      s"""bash -c '${workDirectory}; cd ${archive}/rootfs/\\$$taskdir && ln -s -t . \\`readlink -f \\$$OLDPWD/${archive}/rootfs/inputs\\`/\\*; cd \\$$OLDPWD/${archive}/rootfs && ln -s \\$$taskdir ./outputs'
    //       """.stripMargin

    // ; cd ${archive}/rootfs/\\$$taskdir && ln \\-s \\-t . \\`readlink \\-f \\$$OLDPWD/${archive}/rootfs/inputs\\`/\\*; cd \\$$OLDPWD/${archive}/rootfs && ln \\-s \\$$taskdir ./outputs
    val linkInOutputs = raw"""bash -c \"ls -lh ./toto.raw\" """

    //      "bash -xc " + "'ls'"  //${workDirectory}" """

    new CARETaskBuilder(Seq(extract, linkInOutputs, reExecute + " " + command).map(
      strCommand ⇒ systemexec.Command(strCommand)
    )).addResource(File(archiveLocation))

  }
}
