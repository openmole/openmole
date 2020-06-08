package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.server.utils._
import org.openmole.plugin.method.evolution
import org.openmole.core.services.Services

class EvolutionAnalysisAPIImpl(services: Services) extends EvolutionAnalysisAPI {

  def analyse(path: SafePath) = {
    import ServerFileSystemContext.project
    import services.workspace
    evolution.Analysis.loadMetadata(path.toFile)
  }

}
