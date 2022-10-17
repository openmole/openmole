package org.openmole.gui.plugin.analysis.evolution

import org.openmole.plugin.method.evolution._
import org.openmole.gui.ext.data.MethodAnalysisPlugin

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client.{ InputFilter }
import scaladget.bootstrapnative.bsn._
import com.raquo.laminar.api.L._
import scaladget.tools.*
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.DataUtils._
import org.openmole.gui.ext.client
import org.openmole.plugin.method.evolution.data.AnalysisData.Convergence
import org.openmole.plugin.method.evolution.data.{ AnalysisData, EvolutionMetadata }

import scala.scalajs.js

import org.openmole.plotlyjs.PlotlyImplicits._
import org.openmole.plotlyjs._
import org.openmole.plotlyjs.all._
import org.openmole.plotlyjs.plotlyConts._
import scala.scalajs.js.JSConverters._


import org.openmole.gui.ext.data.*

object TopLevelExports {
  @JSExportTopLevel("evolution")
  val egi = js.Object {
    new org.openmole.gui.plugin.analysis.evolution.EvolutionAnalysis
  }
}

class EvolutionAnalysis extends MethodAnalysisPlugin {

  override def panel(safePath: SafePath, services: PluginServices): HtmlElement = {
    val metadata: Var[Option[Convergence]] = Var(None)

    PluginFetch.future(_.analyse(safePath).future).foreach {
      case Right(m) ⇒ metadata.set(Some(m))
      case Left(e)  ⇒ services.errorManager.signal("Error in evolution analysis", Some(ErrorData.stackTrace(e)))
    }

    div(
      child <-- metadata.signal.map {
        _ match {
          case None ⇒ p("")
          case Some(value) ⇒
            value match {
//              case c: AnalysisData.StochasticNSGA2.Convergence ⇒
//                val layout = Layout
//                  .title("Hypervolume")
//                  .yaxis(Axis.title("Hypervolume"))
//                  .xaxis(Axis.title("Generation"))
//
//                val hvData =
//                  c.generations.flatMap { g ⇒
//                    g.hypervolume.map(hv ⇒ g.generation -> hv)
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
              case _ ⇒ p(value.toString)
            }
        }
      }
    )
  }
}
