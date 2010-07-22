/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.task.external

import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import java.io.File
import org.openmole.commons.tools.io.FileUtil._
import org.openmole.commons.tools.io.IFileOperation
import java.net.URI
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext
import java.util.TreeSet
import scala.collection.mutable.ListBuffer

import scala.collection.JavaConversions._
import org.openmole.plugin.task.external.internal.Activator._


abstract class ExternalSystemTask(name: String) extends ExternalTask(name) {

  def prepareInputFiles(context: IContext, progress: IProgress, tmpDir: File) {
    Logger.getLogger(getClass.getName).log(Level.FINE, "Prepare input files.")

    
    listInputFiles(context, progress).foreach( f => {
        val to = new File(tmpDir, f.name)

        Logger.getLogger(getClass.getName).log(Level.FINE, "Copying {0} to {1}.", Array[Object](f.file, to))
        copy(f.file, to)

        applyRecursive(to, new IFileOperation() {
            override def execute(file: File) =  {
              if (file.isFile()) {
                file.setExecutable(true)
              }
              file.deleteOnExit
            }
          })
      }
    )
  }


  def fetchOutputFiles(context: IContext, progress: IProgress, localDir: File) = {
    val usedFiles = new TreeSet[File]

    setOutputFilesVariables(context,progress,localDir).foreach( f => {
        if (!f.file.exists) {
          throw new UserBadDataError("Output file " + f.file.getAbsolutePath + " for task " + getName + " doesn't exist")
        }
        usedFiles add (f.file)
      }
    )

    val unusedFiles = new ListBuffer[File]
    val unusedDirs = new ListBuffer[File]

    applyRecursive(localDir, new IFileOperation() {
        override def execute(file: File) =  {
          if(file.isFile) unusedFiles += (file)
          else unusedDirs += (file)
        }
      }, usedFiles)

    unusedFiles.foreach( f => {
      f.delete
    })

    //TODO algorithm is no optimal and may be problematic for a huge number of dirs
    unusedDirs.foreach( d => {
      if(d.exists && !usedFiles.contains(d) && dirContainsNoFileRecursive(d)) {
        recursiveDelete(d)
      }
    })
  }
}
