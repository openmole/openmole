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

import org.openmole.core.tools.io.FileUtil
import FileUtil._
import org.scalajs.core.tools.io._
import org.scalajs.core.tools.logging._
import org.scalajs.core.tools.classpath._
import org.scalajs.core.tools.classpath.builder._
import org.scalajs.core.tools.optimizer._
import org.osgi.framework.Bundle
import scala.collection.JavaConverters._
import java.io.{ FileOutputStream, File }

object JSPack {

  val JS_FILE = "plugins.js"

  def apply(src: File, target: File, optimized: Boolean = true) = {

    //FIXME: get the jar from bundles
    val scalajsLib = copyJar("scalajs-library_2.11-0.6.1")

    val semantics = org.scalajs.core.tools.sem.Semantics.Defaults

    val partialClasspath = PartialClasspathBuilder.build(collection.immutable.Seq(scalajsLib, src))

    val completeClasspath = partialClasspath.resolve()

    val optimizer = new ScalaJSOptimizer(semantics)

    val logger = new ScalaConsoleLogger

    val out = WritableFileVirtualJSFile(new java.io.File(target, JS_FILE))
    if (optimized) {
      val sems = semantics.optimized

      new ScalaJSClosureOptimizer(sems).optimizeCP(
        new ScalaJSOptimizer(sems),
        completeClasspath,
        ScalaJSClosureOptimizer.Config(out),
        logger
      )
    }
    else {
      optimizer.optimizeCP(
        completeClasspath,
        ScalaJSOptimizer.Config(out, checkIR = false, wantSourceMap = !optimized),
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