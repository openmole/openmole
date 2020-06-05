package org.openmole.gui.plugin.analysis.evolution

import org.openmole.plugin.method.evolution._
import org.openmole.gui.ext.data.MethodAnalysisPlugin
import org.scalajs.dom.raw.HTMLElement
import scalatags._
import scalatags.JsDom.all._

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("org.openmole.gui.plugin.analysis.evolution.EvolutionAnalysis")
class EvolutionAnalysis extends MethodAnalysisPlugin {
  override val panel: JsDom.TypedTag[HTMLElement] = p("evolve !")
}
