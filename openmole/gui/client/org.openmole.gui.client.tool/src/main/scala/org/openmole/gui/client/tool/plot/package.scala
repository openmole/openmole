package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._
import com.definitelyscala.plotlyjs.PlotlyImplicits._
import scalatags.JsDom.all._

case class BasePlot(
  title:   String        = "",
  serie:   Serie,
  legend:  Boolean       = false,
  plotter: Plotter,
  error:   Option[Serie]) {

  def scatter = ScatterPlot(title, serie, legend, plotter, error)

  def line = XYPlot(title, serie, legend, plotter, error)

  def splom = SplomPlot(title, serie, legend, plotter)

  def heatmap = HeatMapPlot(title, serie, legend)
}

object Plot {

  case class ToBePloted(indexes: Seq[Int])

  object PlotDimension {
    def plotModes(plotDimension: PlotDimension): Seq[PlotMode] =
      plotDimension match {
        case ColumnPlot ⇒ Seq(ScatterMode, SplomMode)
        case LinePlot   ⇒ Seq(XYMode, HeatMapMode)
      }
  }

  sealed trait PlotDimension

  object ColumnPlot extends PlotDimension

  object LinePlot extends PlotDimension

  sealed trait PlotMode {
    def name: String
  }

  object XYMode extends PlotMode {
    def name = "1 row = 1 plot"
  }

  object ScatterMode extends PlotMode {
    def name = "Scatter"
  }

  object SplomMode extends PlotMode {
    def name = "SPLOM"
  }

  object HeatMapMode extends PlotMode {
    def name = "Heat map"
  }

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
    title:   String        = "",
    serie:   Serie         = Serie(),
    legend:  Boolean       = false,
    plotter: Plotter,
    error:   Option[Serie] = None) = {
    val bpl = BasePlot(title, serie, legend, plotter, error)
    plotter.plotMode match {
      case XYMode      ⇒ bpl.line
      case ScatterMode ⇒ bpl.scatter
      case SplomMode   ⇒ bpl.splom
      case HeatMapMode ⇒ bpl.heatmap
    }
  }
}