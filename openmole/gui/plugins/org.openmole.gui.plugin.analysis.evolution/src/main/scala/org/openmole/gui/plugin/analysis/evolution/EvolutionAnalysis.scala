package org.openmole.gui.plugin.analysis.evolution

import org.openmole.plugin.method.evolution._
import org.openmole.gui.ext.data.MethodAnalysisPlugin

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client.{ InputFilter, OMPost }
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._
import org.scalajs.dom.raw.HTMLElement
import scaladget.bootstrapnative.SelectableButtons

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.DataUtils._
import org.openmole.gui.ext.client
import org.openmole.plugin.method.evolution.data.AnalysisData.Convergence
import org.openmole.plugin.method.evolution.data.{ AnalysisData, EvolutionMetadata }
import rx._

import scala.scalajs.js

import org.openmole.plotlyjs.PlotlyImplicits._
import org.openmole.plotlyjs._
import org.openmole.plotlyjs.all._
import org.openmole.plotlyjs.plotlyConts._
import scala.scalajs.js.JSConverters._

object TopLevelExports {
  @JSExportTopLevel("evolution")
  val egi = js.Object {
    new org.openmole.gui.plugin.analysis.evolution.EvolutionAnalysis
  }
}

class EvolutionAnalysis extends MethodAnalysisPlugin {

  override def panel(safePath: SafePath, services: PluginServices): TypedTag[HTMLElement] = {
    val metadata: Var[Option[Convergence]] = Var(None)

    OMPost()[EvolutionAnalysisAPI].analyse(safePath).call().foreach {
      case Right(m) ⇒ metadata() = Some(m)
      case Left(e)  ⇒ services.errorManager.signal("Error in evolution analysis", Some(ErrorData.stackTrace(e)))
    }

    div(
      Rx {
        metadata() match {
          case None ⇒ p("").render
          case Some(value) ⇒
            value match {
              case c: AnalysisData.StochasticNSGA2.Convergence ⇒
                val layout = Layout
                  .title("Hypervolume")
                  .yaxis(Axis.title("Hypervolume"))
                  .xaxis(Axis.title("Generation"))

                val hvData =
                  c.generations.flatMap { g ⇒
                    g.hypervolume.map(hv ⇒ g.generation -> hv)
                  }

                val data = PlotData
                  .x(hvData.map(_._1.toDouble).toJSArray)
                  .y(hvData.map(_._2).toJSArray)
                //.customdata((1 to 10).toJSArray.map(_.toString))
                //                  .set(plotlymode.markers)
                //                  .set(plotlytype.scatter)
                //                  .set(plotlymarker
                //                    //.set(plotlysizemode.area)
                //                    .size(15)
                //                    .set(plotlycolor.array(colorDim.toJSArray))
                //                    .set(Color.rgb(40, 125, 255))
                //                  )
                //

                val config = Config.displayModeBar(false)
                val plotDiv = div().render
                Plotly.plot(plotDiv, js.Array(data), layout, config = config)
                plotDiv
              case _ ⇒ p(value.toString).render
            }

        }
      }
    )
  }
}
