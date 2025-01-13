package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs
import org.openmole.plotlyjs.*
import org.openmole.plotlyjs.all.*
import org.openmole.plotlyjs.PlotlyImplicits.*

import scala.scalajs.js.JSConverters.*
import scala.scalajs.js
import com.raquo.laminar.api.L.*
object ParallelPlot:

  def apply(
    content: Seq[Seq[String]],
    titles: Seq[String],
    plotSettings: PlotSettings,
    legend: Boolean = false
   ) =
    lazy val plotDiv = Plot.baseDiv

    lazy val layout = Layout.width(1200)

    val dimensions = content.zipWithIndex.map { case (c,id) =>

      val dim =
        Tools.parseDouble(c.head) match {
        case Some(_)=> dimension.values(c.toJSArray)
        case None=>
          val mapIndex = c.distinct.zipWithIndex.toMap
          val indexSeq = mapIndex.toJSArray
          val values = c.map(s=> mapIndex(s)).toJSArray
          val tickVals = indexSeq.map(_._2)
          val text = indexSeq.map(_._1)
          dimension.values(values).tickVals(tickVals).tickText(text)
      }

      dim
        .label(titles(id))
        ._result

    }.toJSArray

    val plotData = parallelCoordinates
      .line(line.color(plotlyjs.all.color.rgb(60, 90, 140)).width(2))
      .marker(marker
        .size(12)
        .color(plotlyjs.all.color.rgba(60, 90, 140, 0.5))
        .symbol(circle)
      )
      .dimensions(dimensions)
      ._result

    Plotly.newPlot(plotDiv.ref, Seq(plotData).toJSArray, layout)

    plotDiv

