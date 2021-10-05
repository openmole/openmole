package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs._
import org.openmole.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._

import com.raquo.laminar.api.L._

object XYPlot {

  def apply(
    title:   String        = "",
    serie:   Serie,
    legend:  Boolean       = false,
    plotter: Plotter,
    error:   Option[Serie]
  ) = {

    lazy val plotDiv = Plot.baseDiv

    // val nbDims = serie.yValues.length
    val nbDims = plotter.toBePlotted.indexes.length

    nbDims match {
      case _ if nbDims < 1 ⇒ div()
      case _ ⇒

        val data = serie.yValues.map { y ⇒
          serie.plotDataBuilder
            .x(serie.xValues.values.toJSArray)
            .y(y.values.toJSArray)._result
        }.toJSArray

        Plotly.newPlot(
          plotDiv.ref,
          data,
          Plot.baseLayout(title).width(800),
          Plot.baseConfig
        )

        plotDiv
    }

  }

}