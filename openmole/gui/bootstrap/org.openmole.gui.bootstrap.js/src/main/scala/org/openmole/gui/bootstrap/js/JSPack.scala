package org.openmole.gui.bootstrap.js

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

import org.openmole.misc.osgi.Activator
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
import java.io.{ FileOutputStream, File }

object JSPack {

  // val OPTIMIZED = "plugins-opt.js"
  val JS_FILE = "plugins.js"

  def apply(src: File, target: File, optimized: Boolean = true) = {

    //FIXME: get the jar from bundles
    val scalajsLib = copyJar("scalajs-library_2.11")

    val partialClasspath = PartialClasspathBuilder.buildIR(collection.immutable.Seq(scalajsLib, src))

    val completeClasspath = partialClasspath.resolve()

    val optimizer = new ScalaJSOptimizer

    val logger = new ScalaConsoleLogger

    val out =
      if (optimized) WritableMemVirtualJSFile("out.js")
      else WritableFileVirtualJSFile(new java.io.File(target, JS_FILE))

    val optimizedClasspath = optimizer.optimizeCP(
      ScalaJSOptimizer.Inputs(completeClasspath),
      ScalaJSOptimizer.OutputConfig(out, checkIR = false, wantSourceMap = !optimized),
      logger
    )

    if (optimized) {
      val fullOptOut = WritableFileVirtualJSFile(new java.io.File(target, JS_FILE))

      val fullOptimizer = new ScalaJSClosureOptimizer
      val fullyOptimizedCP = fullOptimizer.optimizeCP(
        ScalaJSClosureOptimizer.Inputs(optimizedClasspath),
        ScalaJSClosureOptimizer.OutputConfig(fullOptOut),
        logger
      )
    }
  }

  def copyJar(name: String) = {
    val libF = File.createTempFile(name, ".jar")
    libF.deleteOnExit
    val outLib = new FileOutputStream(libF)
    getClass.getClassLoader.getResourceAsStream(name + ".jar").copy(outLib)
    libF
  }
}