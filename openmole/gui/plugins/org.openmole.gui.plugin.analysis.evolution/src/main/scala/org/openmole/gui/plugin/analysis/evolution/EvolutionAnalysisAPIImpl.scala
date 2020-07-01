package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.data._
import org.openmole.plugin.method.evolution._
import org.openmole.core.services.Services
import org.openmole.gui.ext.server.utils._
import org.openmole.plugin.method.evolution.data._

class EvolutionAnalysisAPIImpl(services: Services) extends EvolutionAnalysisAPI {

  def load(path: SafePath): Either[ErrorData, EvolutionMetadata] = {
    import ServerFileSystemContext.project
    import services.workspace

    try Right(Analysis.loadMetadata(path.toFile))
    catch {
      case e ⇒ Left(ErrorData(e))
    }
  }

}
