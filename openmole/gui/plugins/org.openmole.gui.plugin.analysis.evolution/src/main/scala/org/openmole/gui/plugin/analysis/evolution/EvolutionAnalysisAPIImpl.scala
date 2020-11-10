package org.openmole.gui.plugin.analysis.evolution

import org.openmole.gui.ext.data._
import org.openmole.plugin.method.evolution._
import org.openmole.core.services.Services
import org.openmole.gui.ext.server.utils._
import org.openmole.plugin.method.evolution.data._

class EvolutionAnalysisAPIImpl(services: Services) extends EvolutionAnalysisAPI {

  def analyse(path: SafePath) = {
    import ServerFileSystemContext.project
    import services._

    try {
      def m = Analysis.loadMetadata(path.toFile)
      Right(Analysis.analyse(m, path.toFile.getParentFile))
    }
    catch {
      case e ⇒ Left(ErrorData(e))
    }
  }

}
