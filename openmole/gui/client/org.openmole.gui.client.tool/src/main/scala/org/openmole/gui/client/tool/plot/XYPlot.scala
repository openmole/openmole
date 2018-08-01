package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.all._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scalatags.JsDom.all._
import Serie._

object XYPlot {

  def apply(
    title:      String     = "",
    xaxisTitle: String     = "",
    yaxisTitle: String     = "",
    series:     Seq[Serie],
    legend:     Boolean    = false) = {

    lazy val plotDiv = div.render

    lazy val layout = Layout
      .title(title)
      .showlegend(legend)
      .xaxis(plotlyaxis.title(xaxisTitle))
      .yaxis(plotlyaxis.title(yaxisTitle))

    lazy val config = Config.displayModeBar(false)

    val plotDataArray: scalajs.js.Array[PlotData] = series

    Plotly.newPlot(
      plotDiv,
      plotDataArray,
      layout,
      config)

    div(plotDiv.render).render
  }
}