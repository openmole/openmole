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

package org.openmole.plugin.task.external

import java.io.File
import java.io.IOException

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
import org.openmole.core.model.data.IContext
import org.openmole.commons.tools.io.FileUtil

import org.openmole.core.implementation.tools.VariableExpansion._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

object ExternalTask {
    val PWD = new Prototype[String]("PWD", classOf[String])
    
    /*def apply(pwd: String): List[IVariable[_]] = {
      val vals = List(new Variable(PWD, pwd))
      vals
    }*/
}


abstract class ExternalTask(name: String) extends Task(name) {
 
  val inContextFiles = new ListBuffer[(IPrototype[File], String)]
  val inContextFileList = new ListBuffer[(IPrototype[java.util.List[File]], IPrototype[java.util.List[String]])]
  val inFileNames = new HashMap[File, String]

  val outFileNames = new ListBuffer[(IPrototype[File], String)]
  val outFileNamesFromVar = new ListBuffer[(IPrototype[File], IPrototype[String])]
  val outFileNamesVar = new HashMap[IPrototype[File], IPrototype[String]]

  protected class ToPut(val file: File, val name: String)
  protected class ToGet(val name: String, val file: File)

  protected def listInputFiles(global: IContext, context: IContext, progress: IProgress): List[ToPut] = {
    var ret = new ListBuffer[ToPut]

    inFileNames.foreach(entry => {
        val localFile = entry._1         
        ret += (new ToPut(localFile, expandData(global, context, entry._2)))
      })

    inContextFiles.foreach( p => {
        context.value(p._1) match {
          case None => throw new UserBadDataError("File supposed to be present in variable \"" + p._1.name + "\" at the beging of the task \"" + name + "\" and is not.")
          case Some(f) => ret += (new ToPut(f, expandData(global, context, p._2)))
        }
      })

    inContextFileList.foreach( p => {
        context.value(p._1) match {
          case None =>
          case Some(lstFile) =>
            context.value(p._2) match {
              case None =>
              case Some(lstName) => {
                  val fIt = lstFile.iterator
                  val sIt = lstName.iterator

                  while (fIt.hasNext && sIt.hasNext) {
                    val f = fIt.next
                    val name = sIt.next

                    ret += (new ToPut(f, expandData(global, context, name)))
                  }
                }
            }
        }
      })
    ret.toList
  }


  protected def setOutputFilesVariables(global: IContext, context: IContext, progress: IProgress, localDir: File): List[ToGet] = {

    var ret = new ListBuffer[ToGet]

    outFileNames.foreach(p => {
        val filename = expandData(global, context, p._2)
        val fo = new File(localDir,filename)

        ret += (new ToGet(filename, fo))

        context += (p._1, fo)
        outFileNamesVar.get(p._1) match {
          case None =>
          case Some(value) => context += (value, filename)
        }
      })

    outFileNamesFromVar foreach ( p => {
        context.value(p._2) match  {
          case None => throw new UserBadDataError("Variable containing the output file name should exist in the context at the end of the task" + name)
          case Some(filename) =>
            val fo = new File(localDir, filename)
            ret += (new ToGet(filename, fo))
            context += (p._1, fo)
        }

      })

    return ret.toList
  }

  def exportFilesFromContextAs(fileList: IPrototype[java.util.List[File]], names: IPrototype[java.util.List[String]]): this.type = {
    inContextFileList += ((fileList, names))
    super.addInput(fileList)
    super.addInput(names)
    this
  }

  def exportFileFromContextAs(fileProt: IPrototype[File], name: String): this.type = {
    inContextFiles += ((fileProt, name))
    super.addInput(fileProt)
    this
  }

  def importFileInContext(v: IPrototype[File], fileName: String): this.type = {
    outFileNames += ((v, fileName))
    addOutput(v)
    this
  }

  def importFileAndFileNameInContext(v: IPrototype[File], varFileName: IPrototype[String], fileName: String): this.type = {
    importFileInContext(v, fileName)
    addOutput(varFileName)
    outFileNamesVar.put(v, varFileName)
    this
  }

  def importFileInContext(v: IPrototype[File], varFileName: IPrototype[String]): this.type = {
    addOutput(v)
    outFileNamesFromVar += ((v, varFileName))
    this
  }

  def addInFile(file: File, name: String): this.type = {
    inFileNames.put(file, name)
    this
  }

  def addInFile(file: File): this.type =  addInFile(file, file.getName)
    
  def addInFile(location: String): this.type = addInFile(new File(location))

  def addInFile(location: String, name: String): this.type =  addInFile(new File(location), name)
  
}
