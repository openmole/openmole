/*
 *  Copyright (C) 2010 mathieu
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

package org.openmole.plugin.task.datasetdistribution

import java.util.Collection

import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IPrototype
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import Utils._

abstract class GenericDatasetDistribution(name: String,
                                          outputDirectoryPath: String,
                                          nbCategories: String,
                                          chartTitle: String,
                                          xLegend: String,
                                          yLegend: String,
                                          imageWidth: Int,
                                          imageHeight: Int) extends Task(name){


  val charts = new ListBuffer[(IPrototype[Collection[Number]], String)]
 
  /**
   * Add a set of data to be ploted. The name of the prototype will be used to
   * name the corresponding output file.
   */
  def addChart(prototype: IPrototype[Collection[Number]]) {
    // addChart(prototype, prototype.getName() + Extension.image);
    addChart(prototype, prototype.getName() + imageExtension);
  }

  /**
   * Add a set of data to be ploted.
   * fileName is the name the corresponding output file.
   */
  def addChart(prototype: IPrototype[Collection[Number]], fileName: String) {
    charts += ((prototype, fileName))
    addInput(prototype)
  }

}
