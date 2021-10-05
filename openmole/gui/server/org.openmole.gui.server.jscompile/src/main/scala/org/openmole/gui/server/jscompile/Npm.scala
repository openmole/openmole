package org.openmole.gui.server.jscompile

import java.io.File

object Npm {

  def install(targetDir: File) = {
    External.run("npm", Seq("install"), targetDir)
  }
}