package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs._
import org.openmole.plotlyjs.all._
import org.openmole.plotlyjs.SplomDataBuilder._
import org.openmole.plotlyjs.PlotlyImplicits._
import scala.scalajs.js.JSConverters._
import scala.scalajs.js
import com.raquo.laminar.api.L._


object SplomPlot {

  def apply(contents: Seq[Seq[String]],
            labels: Seq[String],
            plotSettings: PlotSettings,
            legend: Boolean = false
           ) =

    lazy val plotDiv = Plot.baseDiv

    val nbDims = contents.size
    if (nbDims > 1) {
      val size = nbDims * (if (nbDims < 3) 200 else 150)
      lazy val layout = Layout
        .showlegend(false)
        .autosize(true)
        .width(1200)
        .font(Font.family("gi").size(14))
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

      val dimensions = contents.zip(labels).map { case (d, label) =>
        Dimension.values(d.toJSArray).label(label)._result
      }.toJSArray

      val arraySize = contents.headOption.map {
        _.length
      }.getOrElse(0)
      val colors = (0 to arraySize).toJSArray map { x => x.toDouble / arraySize }

      val data = splom
        .set(dimensions)
        .showupperhalf(false)
        .set(marker
          .color(Color.array(colors))
          .showscale(true)
        )

      Plotly.newPlot(
        plotDiv.ref,
        scalajs.js.Array(data),
        layout,
        Plot.baseConfig
      )
    }

    plotDiv
}
