package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs._
import org.openmole.plotlyjs.all._
import org.openmole.plotlyjs.PlotlyImplicits._
import org.openmole.plotlyjs.plotlyConts._

import scala.scalajs.js.JSConverters._

case class Dim(values: Seq[String], label: String = "") {
  def toDimension = Dimension.values(values.toJSArray).label(label)
}

case class Serie(
  // dimensionSize:   Int               = 0,
  xValues:         Dim               = Dim(Seq()),
  yValues:         Array[Dim]        = Array(),
  plotDataBuilder: PlotDataBuilder   = linechart.lines,
  markerBuilder:   PlotMarkerBuilder = marker.symbol(cross),
  colorScale:      ColorScale        = colorscale.blues
)