package org.openmole.site

import java.io.File

import org.openmole.tool.file._

import scala.collection.mutable.ListBuffer

object Test:


  opaque type TestList = Option[ListBuffer[Test]]

  var testing = false
  var allTests = ListBuffer[Test]()

  def list(test: Test) =
    if Test.testing then Test.allTests += test

  def generate(target: File) = synchronized:
    allTests.clear()
    testing = true

    val tests =
      try
        Pages.all.foreach { _.content }
        allTests.toVector.distinct
      finally
        allTests.clear()
        testing = false

    target.mkdirs()

    for
      (t, i) ← tests.zipWithIndex
    do
      def name: String = t.name.getOrElse(s"test${i}")

      (target / s"${i}_$name.omt").content =
        s"""${t.code}
           |EmptyTask()
         """.stripMargin


//  def urls = {
//    val tests = shared.links.map { l ⇒
//      println("Testing " + l)
//      val rep = Http(l).timeout(connTimeoutMs = 5000, readTimeoutMs = 60000).asString
//      (l, rep.code)
//    }
//
//    println("\nRESULT:")
//    val failed = tests.filter { _._2 >= 400 }
//    failed.isEmpty match {
//      case true ⇒ println(s"The ${shared.links.size} external links are valid")
//      case false ⇒ failed.foreach { l ⇒
//        println(s"${l._1} is invalid | ${l._2}")
//      }
//    }
//  }


case class Test(code: String, name: Option[String])
