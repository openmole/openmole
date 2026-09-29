package org.openmole.gui.plugin.visualization.evolution

import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.tool.bootstrapnative.bsn.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*
import org.openmole.plotlyjs.*
import org.openmole.plotlyjs.PlotlyImplicits.*
import org.openmole.plotlyjs.all.*
import org.openmole.plotlyjs.plotlyConts.*
import org.openmole.plugin.method.evolution.*
import org.openmole.plugin.method.evolution.AnalysisData.Convergence
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.*

object TopLevelExports:
  @JSExportTopLevel("visualization_evolution")
  val egi = js.Object:
    new org.openmole.gui.plugin.visualization.evolution.EvolutionVisualization


class EvolutionVisualization extends VisualizationPlugin:

  def sttp = STTPInterpreter()

  override def panel(safePath: SafePath, services: PluginServices)(using basePath: BasePath, notificationAPI: NotificationService): HtmlElement =
    val metadata: Var[Option[Convergence]] = Var(None)

    sttp.toRequest(EvolutionVisualizationAPI.analyse)(safePath).foreach: m =>
      metadata.set(Some(m))

    div(
      child <-- metadata.signal.map:
        case None => p("")
        case Some(value) =>
          value match
  //              case c: AnalysisData.StochasticNSGA2.Convergence =>
  //                val layout = Layout
  //                  .title("Hypervolume")
  //                  .yaxis(Axis.title("Hypervolume"))
  //                  .xaxis(Axis.title("Generation"))
  //
  //                val hvData =
  //                  c.generations.flatMap { g =>
  //                    g.hypervolume.map(hv => g.generation -> hv)
  //                  }
  //
  //                val data = PlotData
  //                  .x(hvData.map(_._1.toDouble).toJSArray)
  //                  .y(hvData.map(_._2).toJSArray)
  //                //.customdata((1 to 10).toJSArray.map(_.toString))
  //                //                  .set(plotlymode.markers)
  //                //                  .set(plotlytype.scatter)
  //                //                  .set(plotlymarker
  //                //                    //.set(plotlysizemode.area)
  //                //                    .size(15)
  //                //                    .set(plotlycolor.array(colorDim.toJSArray))
  //                //                    .set(Color.rgb(40, 125, 255))
  //                //                  )
  //                //
  //
  //                val config = Config.displayModeBar(false)
  //                val plotDiv = div()
  //                Plotly.plot(plotDiv.ref, js.Array(data), layout, config = config)
  //                plotDiv
            case _ => p(value.toString)
    )

