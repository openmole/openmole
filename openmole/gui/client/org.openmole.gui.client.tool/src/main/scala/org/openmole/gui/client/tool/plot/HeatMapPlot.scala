package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.all._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import scalatags.JsDom.all._

object HeatMapPlot {

  def apply(
    title:  String  = "",
    serie:  Serie,
    legend: Boolean = false) = {

    lazy val plotDiv = Plot.baseDiv

    val dims = serie.values.map { _.values.reverse }.transpose

    val data = PlotData
      .z(dims.map { _.toJSArray }.toJSArray)
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
