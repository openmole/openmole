package org.openmole.gui.client.tool.plot

import org.openmole.plotlyjs._
import org.openmole.plotlyjs.PlotlyImplicits._
import com.raquo.laminar.api.L._
import org.openmole.plotlyjs.Margin._

//case class BasePlot(
//  title:   String        = "",
//  serie:   Serie,
//  legend:  Boolean       = false,
//  plotter: Plotter,
//  error:   Option[Serie]) {
//
//  def scatter = ScatterPlot(title, serie, legend, plotter, error)
//
//  def line = XYPlot(title, serie, legend, plotter, error)
//
//  def splom = SplomPlot(title, serie, legend, plotter)
//
//  def heatmap = HeatMapPlot(title, serie, legend)
//}

object Plot {

  case class ToBePloted(indexes: Seq[Int])

  object PlotDimension {
    def plotModes(plotDimension: PlotDimension): Seq[PlotMode] =
      plotDimension match {
        case ColumnPlot ⇒ Seq(ScatterMode, SplomMode, XYMode)
        case LinePlot   ⇒ Seq(HeatMapMode)
      }
  }

  sealed trait PlotDimension

  object ColumnPlot extends PlotDimension

  object LinePlot extends PlotDimension

  sealed trait PlotMode {
    def name: String
  }

  object XYMode extends PlotMode {
    def name = "Series"
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

  sealed trait NumberOfColumToBePlotted
  object OneColumn extends NumberOfColumToBePlotted
  object TwoColumn extends NumberOfColumToBePlotted
  object NColumn extends NumberOfColumToBePlotted
  
  def baseDiv:Div = div()

  val baseConfig = Config
    .displayModeBar(false)
    .responsive(true)

  val axis = Axis.zeroline(false).showline(false)

  def baseLayout(xTitle: String, yTitle: String) = Layout
    .showlegend(false)
    .autosize(true)
    .xaxis(axis.title(xTitle))
    .yaxis(axis.title(yTitle))
    .font(Font.family("gi").size(14))


  case class LayoutedPlot(element: HtmlElement, layout: Layout)

//  def apply(
//    title:   String        = "",
//    serie:   Serie         = Serie(),
//    legend:  Boolean       = false,
//    plotter: Plotter,
//    error:   Option[Serie] = None) = {
//    val bpl = BasePlot(title, serie, legend, plotter, error)
//    plotter.plotMode match {
//      case XYMode      ⇒ bpl.line
//      case ScatterMode ⇒ bpl.scatter
//      case SplomMode   ⇒ bpl.splom
//      case HeatMapMode ⇒ bpl.heatmap
//    }
//  }
}