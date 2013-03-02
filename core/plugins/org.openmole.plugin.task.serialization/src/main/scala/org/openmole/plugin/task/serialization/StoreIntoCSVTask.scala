/*
 *  Copyright (C) 2010 Mathieu Leclaire
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
package org.openmole.plugin.task.serialization;

import au.com.bytecode.opencsv.CSVWriter
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import org.openmole.misc.tools.io.Prettifier._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.exception._
import org.openmole.misc.workspace._
import collection.JavaConversions._

/**
 * The StoreIntoCSVTask task is dedicated to the storage of data of the workflow
 * into CSV files. It is in particular possible to store data (as prototypes) aggregated in an
 * array. The number of data to store in columns is not limited.
 */

object StoreIntoCSVTask {

  def apply(name: String, outputFile: Prototype[File])(implicit plugins: PluginSet) =
    new StoreIntoCSVTaskBuilder { builder ⇒

      addOutput(outputFile)

      def toTask = new StoreIntoCSVTask(name, outputFile, builder.columns) with builder.Built

    }

}

sealed abstract class StoreIntoCSVTask(
    val name: String,
    val filePrototype: Prototype[File],
    val columns: Iterable[(Prototype[Array[_]], String)]) extends Task {

  override def process(context: Context) = {
    val valuesList = columns.map { elt ⇒ context(elt._1) }

    val file = Workspace.newFile("storeIntoCSV", ".csv")
    val writer = new CSVWriter(new BufferedWriter(new FileWriter(file)), ',', CSVWriter.NO_QUOTE_CHARACTER)

    try {
      //header
      val columnIts = valuesList.map { _.iterator }

      writer.writeNext(columns.map(_._2).toArray)
      val listSize = valuesList.map { _.size }.min

      //body
      for (i ← 0 until listSize)
        writer.writeNext(columnIts.map { e ⇒ e.next.prettify() }.toArray)

    } finally writer.close
    context + (filePrototype -> file)
  }

}
