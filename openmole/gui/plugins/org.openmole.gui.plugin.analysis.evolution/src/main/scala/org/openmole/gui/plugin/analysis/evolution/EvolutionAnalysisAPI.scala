package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.data._
import org.openmole.plugin.method.evolution.data._

trait EvolutionAnalysisAPI {
  def load(path: SafePath): Either[ErrorData, EvolutionMetadata]
}
