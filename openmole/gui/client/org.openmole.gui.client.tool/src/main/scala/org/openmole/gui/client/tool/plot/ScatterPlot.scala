package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs._
import org.openmole.plotlyjs.all._
import org.openmole.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import scala.scalajs.js
import com.raquo.laminar.api.L._

object ScatterPlot {

  def apply(
    title:   String        = "",
    serie:   Serie,
    legend:  Boolean       = false,
    plotter: Plotter,
    error:   Option[Serie]) = {
    lazy val plotDiv = Plot.baseDiv

    val nbDims = plotter.toBePlotted.indexes.length

    //    lazy val baseLayout = Plot.baseLayout(title)
    //      .width(800)
    //      .xaxis(axis.title(serie.xValues.label))
    //
    //    val layout = {
    //      if (nbDims == 2)
    //        baseLayout
    //          .xaxis(axis.title(serie.xValues.label))
    //          .yaxis(axis.title(serie.yValues.head.label))
    //      else baseLayout
    //    }
    //
    //    val data = serie.yValues.map { y ⇒
    //      serie.plotDataBuilder
    //        .set(plottype.scatter).set(plotmode.markers)
    //        .x(serie.xValues.values.toJSArray)
    //        .y(y.values.toJSArray)
    //    }.toJSArray
    //
    //    val plotDataArray = {
    //      if (nbDims == 2) js.Array(ToolPlot.error(data.head, error)._result)
    //      else js.Array[PlotData]()
    //    }

    //  Plotly.newPlot(plotDiv.ref, plotDataArray, layout, Plot.baseConfig)

    plotDiv

  }
}