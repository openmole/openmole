package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs.all._
import scala.scalajs.js.JSConverters._
import com.definitelyscala.plotlyjs.{ PlotData, PlotDataBuilder, PlotMarkerBuilder }

case class Serie(
  name:            String            = "",
  x:               Array[Double]     = Array(),
  y:               Array[Double]     = Array(),
  plotDataBuilder: PlotDataBuilder   = PlotData.set(plotlymode.markers.lines),
  markerBuilder:   PlotMarkerBuilder = plotlymarker.set(plotlysymbol.cross))

object Serie {
  implicit def serieToPlotData(serie: Serie): PlotData = serie.plotDataBuilder
    .x(serie.x.toJSArray)
    .y(serie.y.toJSArray)
    .set(serie.markerBuilder)
    .name(serie.name)

  implicit def seriesToPlotDatas(series: Seq[Serie]): scalajs.js.Array[PlotData] = series.map { serieToPlotData }.toJSArray
}