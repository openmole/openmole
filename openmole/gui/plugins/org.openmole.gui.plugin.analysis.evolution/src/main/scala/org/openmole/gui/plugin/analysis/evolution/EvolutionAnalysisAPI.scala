package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.data._
import org.openmole.plugin.method.evolution.data._

trait EvolutionAnalysisAPI {
  def analyse(path: SafePath): Either[ErrorData, AnalysisData.Convergence]
  def generation(path: SafePath, generation: Option[Long] = None, all: Boolean = false): Either[ErrorData, Seq[AnalysisData.Generation]]
}
