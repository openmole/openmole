package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.all._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import scalatags.JsDom.all._
import scala.scalajs.js

object HeatMapPlot {

  def apply(
    title:  String  = "",
    serie:  Serie,
    legend: Boolean = false) = {

    lazy val plotDiv = Plot.baseDiv

    val dims: js.Array[js.Array[String]] = serie.yValues.map { _.values.toJSArray }.reverse /*.reverse.toArray }.transpose.map { _.toJSArray }*/ .toJSArray

    val data = PlotData
      .z(dims)
      .set(plotlytype.heatmap)

    val layout = Layout
      .title(title)
      .height(700)
      .width(700)

    Plotly.newPlot(
      plotDiv,
      scalajs.js.Array(data),
      layout,
      Plot.baseConfig
    )

    div(plotDiv.render).render
  }
}
