/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.external.system

import org.openmole.misc.exception.UserBadDataError
import java.io.File
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.data.IContext
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ListBuffer

import org.openmole.plugin.task.external.ExternalTask
import scala.collection.JavaConversions._

abstract class ExternalSystemTask(name: String) extends ExternalTask(name) {

  def prepareInputFiles(context: IContext, progress: IProgress, tmpDir: File) {
   
    listInputFiles(context, progress).foreach(f => {        
        val to = new File(tmpDir, f.name)
        
        to.getAbsoluteFile.getParentFile.mkdirs
        f.file.copy(to)

        to.applyRecursive{f => 
          if (f.isFile) f.setExecutable(true)
          f.deleteOnExit
        }
      })
  }


  def fetchOutputFiles(context: IContext, progress: IProgress, localDir: File) = {
    var usedFiles = new TreeSet[File]

    setOutputFilesVariables(context,progress,localDir).foreach(f => {
        if (!f.file.exists) 
          throw new UserBadDataError("Output file " + f.file.getAbsolutePath + " for task " + name + " doesn't exist")
        usedFiles += f.file
      }
    )

    val unusedFiles = new ListBuffer[File]
    val unusedDirs = new ListBuffer[File]

    localDir.applyRecursive(f => {
        if(f.isFile) unusedFiles += f
        else unusedDirs += f
      }, usedFiles)

    unusedFiles.foreach(f => f.delete)

    //TODO algorithm is no optimal and may be problematic for a huge number of dirs
    unusedDirs.foreach{d => if(d.exists && !usedFiles.contains(d) && d.dirContainsNoFileRecursive) d.recursiveDelete}
  }
}
