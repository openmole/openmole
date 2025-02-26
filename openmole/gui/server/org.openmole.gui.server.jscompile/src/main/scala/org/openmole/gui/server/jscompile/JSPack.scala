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
import org.scalajs.linker.interface._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.scalajs.linker._

import java.io.File
import org.openmole.tool.file._
import org.openmole.core.workspace._
import org.openmole.gui.server.jscompile.Webpack.ExtraModule
import org.scalajs.logging.{NullLogger, ScalaConsoleLogger}
import org.openmole.core.networkservice.*

object JSPack:

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def link(inputDirectory: File, outputJSFile: File, optimizedJS: Boolean)(using newFile: TmpDirectory): Unit =
    newFile.withTmpFile("lib", ".jar") { jar =>

      JSPack.getClass.getClassLoader.getResourceAsStream("scalajs-library.jar") copy jar

      // Obtain VirtualScalaJSIRFile's from the input classpath
      val irCache = StandardImpl.irFileCache().newCache

      val result =
        for
          (containers, _) ← PathIRContainer.fromClasspath(Seq(jar.toPath, inputDirectory.toPath))
          sjsirFiles ← irCache.cached(containers)
          config = StandardConfig()
            .withSourceMap(true)
            .withOptimizer(optimizedJS)
            .withClosureCompiler(optimizedJS)
            .withModuleKind(ModuleKind.CommonJSModule)
            .withParallel(true)

          linker = StandardImpl.linker(config)
          _ ← linker.link(sjsirFiles, Nil, PathOutputDirectory(outputJSFile.getParentFile), new ScalaConsoleLogger)
        yield ()

      Await.result(result, Duration.Inf)
    }

  def webpack(entryJSFile: File, nodeModulesFile: File, webpackConfigTemplateLocation: File, webpackOutputFile: File, extraModules: Seq[ExtraModule])(using newFile: TmpDirectory, networkService: NetworkService) = {
    newFile.withTmpDir { targetDir =>
      org.openmole.tool.archive.Zip.unzip(nodeModulesFile, targetDir, overwrite = true)

      //3- build the js deps with webpack
      Webpack.run(
        entryJSFile,
        webpackConfigTemplateLocation,
        targetDir,
        webpackOutputFile,
        extraModules
      )
    }
  }
