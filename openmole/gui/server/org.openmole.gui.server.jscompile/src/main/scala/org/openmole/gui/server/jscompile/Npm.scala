package org.openmole.gui.server.jscompile

import java.io.File

object Npm {

  def install(targetDir: File) = {
    //External.syncLockfile("package-lock.json", targetDir) {
    External.run("npm", Seq("install"), targetDir)
    //}
  }
}