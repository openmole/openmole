package org.openmole.gui.server.jscompile

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

import org.openmole.tool.file._

import java.io.File

import org.scalajs.core.tools.io._
import org.scalajs.core.tools.sem._
import org.scalajs.core.tools.linker.backend.{ OutputMode, ModuleKind }
import org.scalajs.core.tools.linker.Linker
import org.scalajs.core.tools.logging.ScalaConsoleLogger

import scala.collection.JavaConverters._
import java.io.{ FileOutputStream, File }

object JSPack {

  def link(inputClasspath: Seq[File], outputJSFile: File): Unit = {
    // Obtain VirtualScalaJSIRFile's from the input classpath
    val irCache = new IRFileCache().newCache
    val irContainers = IRFileCache.IRContainer.fromClasspath(inputClasspath)
    val sjsirFiles = irCache.cached(irContainers)

    // A bunch of options. Here we use all the defaults
    val semantics = Semantics.Defaults
    val outputMode = OutputMode.Default
    val moduleKind = ModuleKind.NoModule
    val linkerConfig = Linker.Config()

    // Actual linking
    val linker = Linker(semantics, outputMode, moduleKind, linkerConfig)
    val logger = new ScalaConsoleLogger
    linker.link(sjsirFiles, WritableFileVirtualJSFile(outputJSFile), logger)
  }

  /* def copyJar(name: String) = {
    val libF = File.createTempFile(name, ".jar")
    libF.deleteOnExit
    val outLib = new FileOutputStream(libF)
    getClass.getClassLoader.getResourceAsStream(name + ".jar").copy(outLib)
    libF
  }*/

}