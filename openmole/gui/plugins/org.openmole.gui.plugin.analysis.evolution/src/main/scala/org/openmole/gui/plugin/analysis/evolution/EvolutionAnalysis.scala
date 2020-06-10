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
import rx._

@JSExportTopLevel("org.openmole.gui.plugin.analysis.evolution.EvolutionAnalysis")
class EvolutionAnalysis extends MethodAnalysisPlugin {
  override def panel(safePath: SafePath): TypedTag[HTMLElement] = {

    val metadata: Var[Option[Either[ErrorData, EvolutionMetadata]]] = Var(None)

    OMPost()[EvolutionAnalysisAPI].load(safePath).call().foreach {
      case Right(m) ⇒ metadata() = Some(Right(m))
      case Left(e)  ⇒ metadata() = Some(Left(e))
    }

    div(
      Rx {
        metadata() match {
          case None               ⇒ p("nop")
          case Some(Right(value)) ⇒ p(value.toString)
          case Some(Left(value))  ⇒ p(scrollableText(ErrorData.stackTrace(value)).view)
        }
      }
    )
  }
}
