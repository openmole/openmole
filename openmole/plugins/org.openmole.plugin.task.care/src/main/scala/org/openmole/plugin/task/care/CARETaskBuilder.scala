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

import java.io.File

import org.openmole.core.workflow.builder.CanBuildTask
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.task.systemexec._
import org.openmole.core.workflow.data._

// arguments to SystemExecTask not really matching the actual one -> set in toTask
abstract class CARETaskBuilder(archiveLocation: String, command: Command, archiveWorkDirectory: String) extends SystemExecTaskBuilder(Seq.empty: _*) { builder ⇒

  /**
   * Input files injection into the archive
   * @param p OpenMOLE variable pointing to the file to inject from the dataflow into the archive
   * @param toArchiveWorkDirectory When set to true, files are copied to the archive's working directory
   *                         whereas a false value will copy from the root of the archive
   */
  override def addInputFile(p: Prototype[File], location: ExpandedString, link: Boolean, toArchiveWorkDirectory: Boolean) =
    if (toArchiveWorkDirectory) super.addInputFile(p, s"rootfs/inputs/${location}", false, true)
    else super.addInputFile(p, s"rootfs/${location}", false, true)

  /**
   * We manage outputs from the working directory of the archive (rootfs/....)
   * @param location file location in the archive
   * @param p  OpenMOLE Val[File] to assign the retrieved file to
   * @param fromArchiveWorkDirectory when set to true, the output file will be retrieved from the working directory
   *                                 of the archive (through a symlink to /outputs)
   *                                 when false, the output file is considered from the root of the archive (rootfs/...)
   */
  override def addOutputFile(location: ExpandedString, p: Prototype[File], fromArchiveWorkDirectory: Boolean) =
    if (fromArchiveWorkDirectory) super.addOutputFile(s"rootfs/outputs/${location}", p, true)
    else super.addOutputFile(s"rootfs/${location}", p, true)

  // TODO override resources

  // TODO handle shallow copies (bind to archive)
  // one option would be to replace call line by bind + call line
  //

  //  def toTask2: CARETask = canBuildTask2.toTask

}
