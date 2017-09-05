package org.openmole.site

import java.io.File
import org.openmole.tool.file._

import scala.collection.mutable.ListBuffer

object Test {

  var testing = false
  var allTests = ListBuffer[Test]()

  def generate(target: File) = synchronized {
    allTests.clear()
    testing = true

    val tests =
      try {
        Pages.all.foreach { _.content }
        allTests.toVector.distinct
      }
      finally {
        allTests.clear()
        testing = false
      }

    target.mkdirs()

    for {
      (t, i) ← tests.zipWithIndex
    } {
      def name: String = t.name.getOrElse(s"test${i}")
      (target / s"${i}_$name.omt").content =
        s"""${t.code}
           |EmptyTask()
         """.stripMargin
    }
  }
}

case class Test(code: String, name: Option[String])
