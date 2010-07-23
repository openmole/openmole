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

import java.io.File
import java.io.IOException
import java.util.HashMap
import java.util.TreeMap

import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.Logger
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext
import org.openmole.commons.tools.io.FileUtil
import org.openmole.commons.tools.io.IFileOperation

import org.openmole.core.implementation.tools.VariableExpansion._

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

abstract class ExternalTask(name: String) extends Task(name) {

  object CommonVariables {
    val PWD = new Prototype[String]("PWD", classOf[String])
    
    def apply(pwd: String): List[IVariable[_]] = {
      val vals = List(new Variable(PWD, pwd))
      vals
    }
  }
 
  val inContextFiles = new ListBuffer[(IPrototype[File], String)]
  val inContextFileList = new ListBuffer[(IPrototype[java.util.List[File]], IPrototype[java.util.List[String]])]
  val inFileNames = new TreeMap[File, String]

  val outFileNames = new ListBuffer[(IPrototype[File], String)]
  val outFileNamesFromVar = new ListBuffer[(IPrototype[File], IPrototype[String])]
  val outFileNamesVar = new HashMap[IPrototype[File], IPrototype[String]]

  protected class ToPut(val file: File, val name: String)
  protected class ToGet(val name: String, val file: File)

  protected def listInputFiles(global: IContext, context: IContext, progress: IProgress): List[ToPut] = {
    try {
      var ret = new ListBuffer[ToPut]

      inFileNames.entrySet().foreach(entry => {
          val localFile = entry.getKey         
          ret += (new ToPut(localFile, expandData(global, context, entry.getValue)))
        })

      inContextFiles.foreach( p => {
          val f = context.getValue(p._1)

          if (f == null) {
            throw new UserBadDataError("File supposed to be present in variable \"" + p._1.getName + "\" at the beging of the task \"" + getName + "\" and is not.")
          }

          ret += (new ToPut(f, expandData(global, context, p._2)))
        })

      inContextFileList.foreach( p => {
          val lstFile = context.getValue(p._1)
          val lstName = context.getValue(p._2)

          if (lstFile != null && lstName != null) {
            val fIt = lstFile.iterator
            val sIt = lstName.iterator

            while (fIt.hasNext && sIt.hasNext) {
              val f = fIt.next
              val name = sIt.next

              ret += (new ToPut(f, expandData(global, context, name)))
            }
          }
        })
      return ret.toList

    } catch {
      case e: IOException => throw new InternalProcessingError(e)
    }
  }

  private def setDeleteOnExit(file: File) = {
    FileUtil.applyRecursive(file, new IFileOperation() {

        override def execute(file: File) = {
          file.deleteOnExit
        }
      })
  }


  protected def setOutputFilesVariables(global: IContext, context: IContext, progress: IProgress, localDir: File): List[ToGet] = {

    var ret = new ListBuffer[ToGet]

    outFileNames.foreach(p => {
        val filename = expandData(global, context, p._2)
        val fo = new File(localDir,filename)

        ret += (new ToGet(filename, fo))

        context.putVariable(p._1, fo)
        if (outFileNamesVar.containsKey(p._1)) {
          context putVariable (outFileNamesVar.get(p._1), filename)
        }
      })

    outFileNamesFromVar foreach ( p => {

        if (!context.contains(p._2)) {
          throw new UserBadDataError("Variable containing the output file name should exist in the context at the end of the task" + getName)
        }

        val filename = context getValue (p._2);
        val fo = new File(localDir, filename)
        ret += (new ToGet(filename, fo))

        context putVariable (p._1, fo)
      })
    setDeleteOnExit(localDir)
    return ret.toList
  }

  def exportFilesFromContextAs(fileList: IPrototype[java.util.List[File]], names: IPrototype[java.util.List[String]]) = {
    inContextFileList += ((fileList, names))
    super.addInput(fileList)
    super.addInput(names)
  }

  def exportFileFromContextAs(fileProt: IPrototype[File], name: String) = {
    inContextFiles += ((fileProt, name))
    super.addInput(fileProt)
  }

  def importFileInContext(v: IPrototype[File], fileName: String) = {
    outFileNames += ((v, fileName))
    addOutput(v)
  }

  def importFileAndFileNameInContext(v: IPrototype[File], varFileName: IPrototype[String], fileName: String) = {
    importFileInContext(v, fileName);
    addOutput(varFileName)
    outFileNamesVar.put(v, varFileName)
  }

  def importFileInContext(v: IPrototype[File], varFileName: IPrototype[String]) = {
    addOutput(v)
    outFileNamesFromVar.add((v, varFileName))
  }

  def addInFile(file: File, name: String): Unit = {
    inFileNames.put(file, name)
  }

  def addInFile(file: File): Unit = {
    addInFile(file, file.getName)
  }
    
  def addInFile(location: String): Unit = {
    addInFile(new File(location))
  }

  def addInFile(location: String, name: String): Unit = {
    addInFile(new File(location), name)
  }
}
