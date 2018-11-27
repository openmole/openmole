package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scalatags.JsDom.all._

case class BasePlot(
  title:  String  = "",
  serie:  Serie,
  legend: Boolean = false) {

  def scatter = ScatterPlot(title, serie, legend)

  def line = XYPlot(title, serie, legend)

  def splom = SplomPlot(title, serie, legend)
}

object Plot {

  sealed trait PlotMode

  object XYMode extends PlotMode

  object ScatterMode extends PlotMode

  object SplomMode extends PlotMode

  def baseDiv = div.render

  val baseConfig = Config
    .displayModeBar(false)
    .responsive(true)

  val axis = Axis.zeroline(false).showline(false)

  def baseLayout(title: String) = Layout
    .title(title)
    .showlegend(false)
    .autosize(true)
    .xaxis(axis)
    .yaxis(axis)
    .margin(com.definitelyscala.plotlyjs.Margin.t(0)._result)

  def apply(
    title:    String   = "",
    serie:    Serie    = Serie(),
    legend:   Boolean  = false,
    plotMode: PlotMode) = {
    val bpl = BasePlot(title, serie, legend)
    plotMode match {
      case XYMode      ⇒ bpl.line
      case ScatterMode ⇒ bpl.scatter
      case SplomMode   ⇒ bpl.splom
    }
  }
}