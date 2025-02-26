package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs
import org.openmole.plotlyjs.*
import org.openmole.plotlyjs.PlotlyImplicits.*

import scala.scalajs.js.JSConverters.*
import org.openmole.plotlyjs.all.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.plot.Plot.LayoutedPlot

object XYPlot:

  def apply(
    yContents: Seq[Seq[String]],
    axisTitles: (String, String),
    plotSettings: PlotSettings,
    legend: Boolean = false) =

    lazy val plotDiv = Plot.baseDiv

    val data = yContents.map { y =>
      val xContent = y.zipWithIndex.map{_._2}.toJSArray
      plotSettings.plotDataBuilder
        .x(xContent)
        .y(y.toJSArray)
        .marker(marker
          .size(12)
          .color(plotlyjs.all.color.rgba(60, 90, 140, 0.5))
          .symbol(circle)
          .line(line.color(plotlyjs.all.color.rgb(60, 90, 140)).width(2))
        )
        .line(line.width(5))
        ._result
    }.toJSArray

    val layout = Plot.baseLayout(axisTitles._1, axisTitles._2)
    Plotly.newPlot(
      plotDiv.ref,
      data,
      layout,
      Plot.baseConfig
    )
    plotDiv
