package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.all._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scalatags.JsDom.all._
import Serie._

object ScatterPlot {

  def apply(
    title:      String     = "",
    xaxisTitle: String     = "",
    yaxisTitle: String     = "",
    series:     Seq[Serie],
    legend:     Boolean    = false) = {
    val plotDiv = div.render

    val plotDataArray: scalajs.js.Array[PlotData] = series.map { serie ⇒
      serie.copy(plotDataBuilder =
        serie.plotDataBuilder
          .set(plotlymode.markers)
          .set(plotlytype.scatter)
      )
    }

    val config = Config.displayModeBar(false)
    Plotly.plot(plotDiv, plotDataArray, config = config)

    div(plotDiv.render).render
  }
}