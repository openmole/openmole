package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs
import org.openmole.plotlyjs.*
import org.openmole.plotlyjs.all.*
import org.openmole.plotlyjs.PlotlyImplicits.*

import scala.scalajs.js.JSConverters.*
import scala.scalajs.js
import com.raquo.laminar.api.L.*

object ScatterPlot {

  def apply(xContent: Seq[String],
            yContents: Seq[String],
            axisTitles: (String, String),
            plotSettings: PlotSettings,
            legend: Boolean = false
           ) = {
    lazy val plotDiv = Plot.baseDiv

    lazy val baseLayout = Plot.baseLayout(axisTitles._1, axisTitles._2)

    val data = scatter
      .x(xContent.toJSArray)
      .y(yContents.toJSArray)
      .marker(marker
        .size(12)
        .color(plotlyjs.all.color.rgba(60, 90, 140, 0.5))
        .symbol(circle)
        .line(line.color(plotlyjs.all.color.rgb(60, 90, 140)).width(2))
      )

    Plotly.newPlot(plotDiv.ref, js.Array(data), baseLayout, Plot.baseConfig)
    plotDiv
  }
}