package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs.all._

import scala.scalajs.js.JSConverters._
import com.definitelyscala.plotlyjs._

case class Dim(values: Seq[String], label: String = "") {
  def toDimension = Dimension.values(values.toJSArray).label(label)
}

case class Serie(
  dimensionSize:   Int               = 0,
  values:          Seq[Dim]          = Seq(),
  plotDataBuilder: PlotDataBuilder   = PlotData.set(plotlymode.markers.lines),
  markerBuilder:   PlotMarkerBuilder = plotlymarker.set(plotlysymbol.cross),
  colorScale:      ColorScale        = ColorScale.blues
)