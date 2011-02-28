/*
 *  Copyright (C) 2010 leclaire
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
package org.openmole.plugin.task.csv;

import au.com.bytecode.opencsv.CSVWriter
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.data.IContext
import org.openmole.misc.exception.UserBadDataError

/**
 * The StoreIntoCSVTask task is dedicated to the storage of data of the workflow
 * into CSV files. It is in particular possible to store data (as prototypes) aggregated in an
 * array. The number of data to store in columns is not limited.
 */
class StoreIntoCSVTask(name: String, fileName: String, var columns: Iterable[(IPrototype[Array[_]], String)],delimiter: Char, quoteChar: Char) extends Task(name) {

  /**
   * Creates an instance of StoreIntoCSVTask with a specific delimiter and
   * quote character
   *
   * @param name, the name of the task
   * @param fileName, the path of the CSV file to be stored
   * @param delimiter, the char to be used to separate values
   * @param quotechar, the char to be used to quote  values
   * @throws UserBadDataError
   * @throws InternalProcessingError
   */
  def this(name: String, fileName: String, delimiter: Char, quotechar: Char) = this(name, fileName, Iterable.empty, delimiter, quotechar)

  /**
   * Creates an instance of StoreIntoCSVTask with a specific delimiter and default
   * quote character (none)
   *
   * @param name, the name of the task
   * @param fileName, the path of the CSV file to be stored
   * @param delimiter, the char to be used to separate values
   * @throws UserBadDataError
   * @throws InternalProcessingError
   */
  def this(name: String, fileName: String, delimiter: Char) = this(name, fileName, delimiter, CSVWriter.NO_QUOTE_CHARACTER)

  
  /**
   * Creates an instance of StoreIntoCSVTask with a default delimiter (',') and
   * quote character (none)
   *
   * @param name, the name of the task
   * @param fileName, the path of the CSV file to be stored
   * @throws UserBadDataError
   * @throws InternalProcessingError
   */
  def this(name: String, fileName: String) = this(name, fileName, ',')



  /**
   * Add a prototype to be stored
   *
   * @param prototype
   */
  def addColumn(prototype: IPrototype[Array[_]]): this.type = addColumn(prototype, prototype.name)

  /**
   * Add a prototype to be stored, specifying explicitely the name of the column header to be saved
   *
   * @param prototype
   * @param columnName, the name of the column header
   */
  def addColumn(prototype: IPrototype[Array[_]], columnName: String): this.type = {
    columns = columns.toList :+ ((prototype, columnName))
    addInput(prototype)
    this
  }

  override def process(context: IContext, progress: IProgress) = {

    try {
      val valuesList = columns.map{elt => context.value(elt._1).getOrElse(throw new UserBadDataError("Variable " + elt._1 + " not found."))}
            
      val file = new File(VariableExpansion.expandData(context, fileName))
      file.getParentFile.mkdirs
      val writer = new CSVWriter(new BufferedWriter(new FileWriter(file)), delimiter, quoteChar)

      try {
        //header
        val columnIts = valuesList.map{_.iterator}

        writer.writeNext( columns.map(_._2).toArray)          
        val listSize = valuesList.map{_.size}.min
            
        //body
        for (i <- 0 until listSize) {
          writer.writeNext(columnIts.map{_.next.toString}.toArray)
        }
      } finally {
        writer.close
      }
    }
  }
}
