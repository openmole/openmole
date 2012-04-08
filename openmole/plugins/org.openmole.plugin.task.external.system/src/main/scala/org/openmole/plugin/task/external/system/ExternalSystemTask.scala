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
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.external.system

import java.io.File
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._
import java.io.FileFilter
import org.openmole.core.model.data.IContext
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

import org.openmole.misc.tools.service.Logger
import org.openmole.plugin.task.external.ExternalTask
import scala.collection.JavaConversions._

object ExternalSystemTask extends Logger

abstract class ExternalSystemTask extends ExternalTask {

  import ExternalSystemTask._
  
  def prepareInputFiles(context: IContext, tmpDir: File) = {
    val links = new ListBuffer[File]
    listInputFiles(context).foreach(
      f => {        
        val to = new File(tmpDir, f.name)        
        to.getAbsoluteFile.getParentFile.mkdirs

        if(f.link) {
          to.createLink(f.file.getAbsolutePath)
          links += to
        } else {
          f.file.copy(to)
          to.applyRecursive{_.deleteOnExit}
        }
      })
    links.toSet
  }


  def fetchOutputFiles(context: IContext, localDir: File, links: Set[File]): IContext = {
    val (resultContext, outputFiles) = listOutputFiles(context,localDir)

    val usedFiles = outputFiles.map(
      f => {
        if (!f.file.exists) throw new UserBadDataError("Output file " + f.file.getAbsolutePath + " for task " + name + " doesn't exist")
        f.file
      }
    ).toSet

    val unusedFiles = new ListBuffer[File]
    val unusedDirs = new ListBuffer[File]

    localDir.applyRecursive (
      f => if(f.isFile) unusedFiles += f else unusedDirs += f,
      usedFiles ++ links
    )

    links.foreach(_.delete)
    unusedFiles.foreach(_.delete)

    //TODO algorithm is no optimal and may be problematic for a huge number of dirs
    unusedDirs.foreach{d => if(d.exists && !usedFiles.contains(d) && d.dirContainsNoFileRecursive) d.recursiveDelete}
    resultContext
  }
  
}
