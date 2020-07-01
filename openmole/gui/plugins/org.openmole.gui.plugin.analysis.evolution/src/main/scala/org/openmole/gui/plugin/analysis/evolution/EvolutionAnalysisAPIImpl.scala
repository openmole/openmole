package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.data._
import org.openmole.plugin.method.evolution
import org.openmole.core.services.Services
import org.openmole.gui.ext.server.utils._
import org.openmole.plugin.method.evolution.data.EvolutionMetadata

class EvolutionAnalysisAPIImpl(services: Services) extends EvolutionAnalysisAPI {

  def load(path: SafePath) = {
    import ServerFileSystemContext.project
    import services.workspace

    try Right(evolution.Analysis.loadMetadata(path.toFile))
    catch {
      case e ⇒ Left(ErrorData(e))
    }
  }

}
