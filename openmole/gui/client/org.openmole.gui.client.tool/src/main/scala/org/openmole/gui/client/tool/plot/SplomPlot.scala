package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs._
import org.openmole.plotlyjs.all._
import org.openmole.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import scalatags.JsDom.all._

object SplomPlot {

  def apply(
    title:   String  = "",
    serie:   Serie,
    legend:  Boolean = false,
    plotter: Plotter) = {

    lazy val plotDiv = Plot.baseDiv

    val dims = serie.yValues
    val nbDims = plotter.toBePlotted.indexes.length

    if (nbDims > 1) {
      val size = nbDims * (if (nbDims < 3) 200 else 150)
      lazy val layout = Plot.baseLayout(title)
        .width(size)
        .height(size)
        .dragmode("select")
        .xaxis2(Plot.axis)
        .yaxis2(Plot.axis)
        .xaxis3(Plot.axis)
        .yaxis3(Plot.axis)
        .xaxis4(Plot.axis)
        .yaxis4(Plot.axis)
        .xaxis5(Plot.axis)
        .yaxis5(Plot.axis)
        .xaxis6(Plot.axis)
        .yaxis6(Plot.axis)
        .xaxis7(Plot.axis)
        .yaxis7(Plot.axis)
        .xaxis8(Plot.axis)
        .yaxis8(Plot.axis)

      val dimensions = dims.map {
        _.toDimension._result
      }.toJSArray

      val arraySize = dims.headOption.map { _.values.length }.getOrElse(0)
      val colors = (0 to arraySize).toJSArray map { x ⇒ x.toDouble / arraySize }

      Plotly.newPlot(
        plotDiv,
        scalajs.js.Array(serie.plotDataBuilder
          .set(dimensions)
          .set(plottype.splom)
          .set(marker
            .color(Color.array(colors))
            .colorScale(ColorScale.viridis)
          )._result
        ),
        layout,
        Plot.baseConfig)
    }

    div(plotDiv.render).render
  }
}
