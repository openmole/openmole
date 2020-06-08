package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.data._
import org.openmole.plugin.method.evolution
import org.openmole.core.services.Services
import org.openmole.gui.ext.server.utils._

class EvolutionAnalysisAPIImpl(services: Services) extends EvolutionAnalysisAPI {

  def analyse(path: SafePath) = {
    import ServerFileSystemContext.project
    import services.workspace
    evolution.Analysis.loadMetadata(path.toFile)
  }

}
