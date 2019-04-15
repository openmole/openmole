package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.all._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scala.scalajs.js
import scalatags.JsDom.all._

object ScatterPlot {

  def apply(
    title:  String        = "",
    serie:  Serie,
    legend: Boolean       = false,
    errors: Option[Serie] = None) = {
    lazy val plotDiv = Plot.baseDiv

    val dims = serie.values.take(2)

    if (dims.length == 2) {
      val data = serie.plotDataBuilder
        .x(dims.head.toDimension._result.values.get)
        .y(dims(1).toDimension._result.values.get)
        .set(plotlymode.markers)
        .set(plotlytype.scatter)

      val plotDataArray: scalajs.js.Array[PlotData] = js.Array(
        ToolPlot.error(data, errors)
      )

      lazy val layout = Plot.baseLayout(title)
        .xaxis(plotlyaxis.title(dims.head.label))
        .yaxis(plotlyaxis.title(dims(1).label))
        .width(800)

      Plotly.newPlot(plotDiv, plotDataArray, layout, Plot.baseConfig)
    }

    div(plotDiv.render).render
  }
}