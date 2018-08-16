package org.openmole.gui.client.tool.plot

case class BasePlot(
  title:      String     = "",
  xaxisTitle: String     = "",
  yaxisTitle: String     = "",
  series:     Seq[Serie],
  legend:     Boolean    = false) {

  def scatter = ScatterPlot(title, xaxisTitle, yaxisTitle, series, legend)
  def line = XYPlot(title, xaxisTitle, yaxisTitle, series, legend)
}

object Plot {

  sealed trait PlotMode
  object XYMode extends PlotMode
  object ScatterMode extends PlotMode

  def apply(
    title:      String     = "",
    xaxisTitle: String     = "",
    yaxisTitle: String     = "",
    series:     Seq[Serie] = Seq(),
    legend:     Boolean    = false,
    plotMode:   PlotMode) = {
    val bpl = BasePlot(title, xaxisTitle, yaxisTitle, series, legend)
    plotMode match {
      case XYMode      ⇒ bpl.line
      case ScatterMode ⇒ bpl.scatter
    }
  }
}