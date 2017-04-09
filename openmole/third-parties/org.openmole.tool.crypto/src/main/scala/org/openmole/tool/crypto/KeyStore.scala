package org.openmole.tool.crypto

import java.io.File

object KeyStore {
  def apply(directory: File, password: String = "openmole") = {
    directory.mkdirs()
    new KeyStore(new File(directory, "keystore"), password)
  }
}

class KeyStore(location: File, password: String) {
  lazy val keyStore = Certificate.loadOrGenerate(location, password)
}
