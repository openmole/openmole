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

import org.openmole.tool.stream._
import org.scalajs.core.tools.io._
import org.scalajs.core.tools.sem._
import org.scalajs.core.tools.linker.backend.{ ModuleKind, OutputMode }
import org.scalajs.core.tools.linker.{ Linker, StandardLinker }
import org.scalajs.core.tools.logging.ScalaConsoleLogger
import java.io.File

import org.openmole.core.workspace._

object JSPack {

  def link(inputDirectory: File, outputJSFile: File)(implicit newFile: NewFile): Unit =
    newFile.withTmpFile("lib", "jar") { jar ⇒
      getClass.getClassLoader.getResourceAsStream("scalajs-library.jar") copy jar

      // Obtain VirtualScalaJSIRFile's from the input classpath
      val irCache = new IRFileCache().newCache
      //val irContainers = IRFileCache.IRContainer.fromJar(Seq(jar, inputDirector))
      val sjsirFiles =
        irCache.cached(
          Seq(IRFileCache.IRContainer.fromJar(jar)) ++ IRFileCache.IRContainer.fromDirectory(inputDirectory)
        )

      // A bunch of options. Here we use all the defaults
      val linkerConfig = StandardLinker.Config().withOptimizer(true).withSourceMap(false)
        .withOptimizer(true)
        .withClosureCompilerIfAvailable(true)

      // Actual linking
      val linker = StandardLinker(linkerConfig)
      val logger = new ScalaConsoleLogger
      linker.link(sjsirFiles, WritableFileVirtualJSFile(outputJSFile), logger)
    }

}