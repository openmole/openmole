package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.all._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import scala.scalajs._
import scalatags.JsDom.all._

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
      case _ if nbDims < 1 ⇒ div.render
      case _ ⇒

        val data = serie.yValues.map { y ⇒
          serie.plotDataBuilder
            .x(serie.xValues.values.toJSArray)
            .y(y.values.toJSArray)._result
        }.toJSArray

        Plotly.newPlot(
          plotDiv,
          data,
          Plot.baseLayout(title).width(800),
          Plot.baseConfig
        )

        div(plotDiv.render).render
    }

  }

}