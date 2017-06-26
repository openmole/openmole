package org.openmole.site

import java.io.File
import org.openmole.tool.file._

import scala.collection.mutable.ListBuffer

object Test {

  var testing = false
  var allTests = ListBuffer[String]()

  def generate(target: File) = synchronized {
    allTests.clear()
    testing = true

    val tests =
      try {
        Pages.all.foreach { _.content }
        allTests.toVector
      }
      finally {
        allTests.clear()
        testing = false
      }

    target.mkdirs()

    for {
      (t, i) ← tests.zipWithIndex
    } (target / s"test${i}.oms").content = t
  }
}
