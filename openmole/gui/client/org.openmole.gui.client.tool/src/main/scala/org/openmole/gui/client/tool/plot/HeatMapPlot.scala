package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs._
import org.openmole.plotlyjs.all._
import org.openmole.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import com.raquo.laminar.api.L._
import scala.scalajs.js

object HeatMapPlot {

  def apply(
    title:  String  = "",
    serie:  Serie,
    legend: Boolean = false) = {

    lazy val plotDiv = Plot.baseDiv

    val dims: js.Array[js.Array[String]] = serie.yValues.map { _.values.toJSArray }.reverse /*.reverse.toArray }.transpose.map { _.toJSArray }*/ .toJSArray

    val data = heatmap
      .z(dims)

    val layout = Layout
      .title(title)
      .height(700)
      .width(700)

    Plotly.newPlot(
      plotDiv.ref,
      scalajs.js.Array(data),
      layout,
      Plot.baseConfig
    )

    plotDiv
  }
}
