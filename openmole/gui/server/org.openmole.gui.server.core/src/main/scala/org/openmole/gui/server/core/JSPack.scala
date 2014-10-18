package org.openmole.gui.server.core

/*
 * Copyright (C) 02/10/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.FileUtil._
import scala.scalajs.tools.io._
import scala.scalajs.tools.logging._
import scala.scalajs.tools.classpath._
import scala.scalajs.tools.classpath.builder._
import scala.scalajs.tools.optimizer._
import scala.scalajs.ir.Infos._
import org.osgi.framework.Bundle
import scala.collection.JavaConverters._
import java.io.File

//import fr.iscpif.scaladget.tools._

object JSPack {

  def apply(bundles: List[Bundle], target: File) = {
    val jsTmpDir = java.nio.file.Files.createTempDirectory("jsFiles").toFile

    bundles.foreach { b ⇒
      println(b.getLocation)
    }

    // Extract and copy all the .sjsir files to jsTmpDir
    bundles.map { b ⇒
      // b.findEntries("/", "*.sjsir", true)
      b.findEntries("/", "*", true)
    }.filterNot {
      _ == null
    }.flatMap {
      _.asScala
    }.map { u ⇒ u.openStream.copy(new java.io.File(jsTmpDir, u.getFile.split("/").tail.mkString("-"))) }

    //JsPack(collection.immutable.Seq(jsTmpDir), target)

    println("YO +++")
    val libF = new java.io.File("/home/mathieu/Bureau/jar/scalajs-library_2.11.jar")
    val rtF = new java.io.File("/home/mathieu/Bureau/jar/rt.jar")
    val pluginF = new java.io.File("/home/mathieu/Bureau/jar/org-openmole-gui-plugin-task-groovy_2.11-3.0-SNAPSHOT.jar")
    //libF.copy(new java.io.File(jsTmpDir, "oo.jar"))
    // you can also add directories which will be traversed for *.sjsir files
    // val cpEntries: scala.collection.immutable.Seq[java.io.File] = jsTmpDir.listFiles.to[collection.immutable.Seq]

    //bundles.map { b ⇒ new java.io.File(b.getLocation) }

    //jsTmpDir.listFiles.to[collection.immutable.Seq] //bundles.to[collection.immutable.Seq] //

    // Traverse JARs/directories and find *.sjsir files. Evaluate JS_DEPENDENCIES
    val partialClasspath = PartialClasspathBuilder.buildIR(collection.immutable.Seq(pluginF, libF))
    println("partial done")

    //val jarClasspath = PartialClasspathBuilder.buildIR(collection.immutable.Seq(new java.io.File("/home/mathieu/Bureau/jar/scalajs-library_2.11.jar"),
    //  new java.io.File("/home/mathieu/Bureau/jar/rt.jar")))
    // Resolve any javascript library dependencies.
    // Fail if some could not be found or dependency graph is not linearlizable
    //partialClasspath.scalaJSIR.map { x ⇒ println("Available par ++ " + x.name) }
    // jarClasspath.scalaJSIR.map { x ⇒ println("Available jar ++ " + x.name) }

    val completeClasspath = partialClasspath. /*merge(jarClasspath).*/ resolve()

    /*completeClasspath.scalaJSIR.foreach { ir ⇒
      println(ir.name)
    }*/
    println("complete done")

    // The Scala.js optimizer (corresponds to fastOptJS)
    val optimizer = new ScalaJSOptimizer

    // Where the optimizer log goes.
    // You might want to implement your own subclass of tools.logging.Logger.
    val logger = new ScalaConsoleLogger

    //Reflection
    val readAllMethod = optimizer.getClass().getMethods.filter { _.getName.contains("readAllData") }.head
    readAllMethod.setAccessible(true)
    val inputs = ScalaJSOptimizer.Inputs(completeClasspath)
    val res = readAllMethod.invoke(optimizer, (inputs.copy(input = inputs.input.scalaJSIR)).input, logger).asInstanceOf[List[ClassInfo]]
    println("RES " + res)
    res.foreach { ci ⇒
      println(ci.encodedName)
    }

    // Place where to store the resulting JS.
    // Use WritableFileVirtualJSFile for physical storage
    // implement your own if you like (stream directly to client?)
    val out = WritableFileVirtualJSFile(new java.io.File(target, "outXX.js"))

    println("before optimizeCP")
    // Create an optimized classpath
    // Look at ScalaJSOptimizer.Inputs and ScalaJSOptimizer.OutputConfig for options
    val optimizedClasspath = optimizer.optimizeCP(
      ScalaJSOptimizer.Inputs(completeClasspath),
      ScalaJSOptimizer.OutputConfig(out, checkIR = false),
      logger
    )
    println("after optimizeCP")

    // Content of fully linked file as string (when using WritableMemVirtualJSFile)
    // out.content

    val fullOptOut = WritableFileVirtualJSFile(new java.io.File(target, "fullOut.js"))

    // Optional step, use GoogleClosure compiler. Smaller code, but slow
    val fullOptimizer = new ScalaJSClosureOptimizer
    val fullyOptimizedCP = fullOptimizer.optimizeCP(
      ScalaJSClosureOptimizer.Inputs(optimizedClasspath),
      ScalaJSClosureOptimizer.OutputConfig(fullOptOut),
      logger
    )

  }
}