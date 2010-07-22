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

package org.openmole.plugin.task.datasetdistribution

import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.xy.XYBarRenderer
import org.jfree.data.statistics.HistogramDataset
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import Utils._

import org.jfree.chart.StandardChartTheme._
import org.jfree.chart.ChartUtilities._
import org.jfree.chart.ChartColor
import org.jfree.chart.ChartFactory._
import scala.collection.JavaConversions._

import org.openmole.core.implementation.tools.VariableExpansion._
/**
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 *
 * The DatasetDistributionTask enables to plot the distribution of a data set.
 * It renders a set of data as an histogram, according to the occurency in each
 * predefined range of values. The number of category, legend names, the ouput
 * image size can be set. It is possible to add as many set as needed. These sets
 * are displayed in separated files as PNG images.
 */
class DatasetDistributionTask (name: String, 
                               outputDirectoryPath: String,
                               nbCategories: String,
                               chartTitle: String,
                               xLegend: String,
                               yLegend: String,
                               imageWidth: Int,
                               imageHeight: Int) extends GenericDatasetDistribution(name,
                                                                                    outputDirectoryPath,
                                                                                    nbCategories,
                                                                                    chartTitle,
                                                                                    xLegend,
                                                                                    yLegend,
                                                                                    imageWidth,
                                                                                    imageHeight) {

  private def createChart(dataset: HistogramDataset, global: IContext, context: IContext): JFreeChart = {
    setChartTheme (createLegacyTheme())

    val chart = createHistogram(expandData(global, context, chartTitle),expandData(global, context, xLegend), expandData(global, context, yLegend), dataset, PlotOrientation.VERTICAL, false, false, false)
    chart getXYPlot() getRenderer() setSeriesPaint(0, ChartColor.VERY_LIGHT_BLUE)

    val plot = chart getXYPlot()
    val renderer:XYBarRenderer = plot.getRenderer().asInstanceOf[XYBarRenderer]
    renderer setDrawBarOutline(true)

    chart setAntiAlias(true)
    chart
  }

  /**
   * Process the task:
   * 1- builds the categories
   * 2- builds a HistogramDataset object
   * 3- saves it as a PNG file
   */
  override def process(global: IContext, context: IContext, progress: IProgress) = {
    try {
      charts foreach ( chart => {
          val data = context getValue(chart._1)
          val array = new Array[Double](data.size)
          var i = 0
          data foreach ( v => {
              array(i) = v.doubleValue
              i += 1
            } )

          val dataset = new HistogramDataset()
          dataset addSeries("", array, expandIntegerData(global, context, nbCategories))

          val jfchart = createChart(dataset, global, context)
          val os = new BufferedOutputStream(new FileOutputStream(expandData(global, context, outputDirectoryPath + "/" + chart._2)));
          try {
            writeChartAsPNG(os, jfchart, imageWidth, imageHeight)
          } finally {
            os.close()
          }
        } )

    } catch {
      case ex: FileNotFoundException => throw new UserBadDataError(ex)
      case ex: IOException => throw new InternalProcessingError(ex)
    }
  }
  
}
