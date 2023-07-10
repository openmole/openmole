package org.openmole.gui.server.jscompile

import java.io.File
import org.openmole.core.networkservice.*

object Npm {

  def install(targetDir: File)(using networkService: NetworkService) =
    External.run("npm", Seq("install"), targetDir, env = NetworkService.proxyVariables)

}