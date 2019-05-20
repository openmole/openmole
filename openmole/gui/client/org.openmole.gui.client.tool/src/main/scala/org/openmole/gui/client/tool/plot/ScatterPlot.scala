package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.all._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import scala.scalajs.js
import scalatags.JsDom.all._

object ScatterPlot {

  def apply(
    title:   String        = "",
    serie:   Serie,
    legend:  Boolean       = false,
    plotter: Plotter,
    error:   Option[Serie]) = {
    lazy val plotDiv = Plot.baseDiv

    val nbDims = plotter.toBePlotted.indexes.length

    lazy val baseLayout = Plot.baseLayout(title)
      .width(800)
      .xaxis(plotlyaxis.title(serie.xValues.label))

    val layout = {
      if (nbDims == 2)
        baseLayout
          .xaxis(plotlyaxis.title(serie.xValues.label))
          .yaxis(plotlyaxis.title(serie.yValues.head.label))
      else baseLayout
    }

    val data = serie.yValues.map { y ⇒
      serie.plotDataBuilder
        .x(serie.xValues.values.toJSArray)
        .y(y.values.toJSArray)
        .set(plotlymode.markers)
        .set(plotlytype.scatter)
    }.toJSArray

    val plotDataArray = {
      if (nbDims == 2) js.Array(ToolPlot.error(data.head, error)._result)
      else js.Array[PlotData]()
    }

    Plotly.newPlot(plotDiv, plotDataArray, layout, Plot.baseConfig)

    div(plotDiv.render).render

  }
}