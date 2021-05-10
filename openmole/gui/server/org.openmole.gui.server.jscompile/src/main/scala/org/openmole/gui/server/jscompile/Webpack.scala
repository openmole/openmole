package org.openmole.gui.server.jscompile

import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.logger.LoggerService

import java.io.{ File, InputStream }
import scala.sys.process.{ BasicIO, Process }

object Webpack extends JavaLogger {

  def run(entry: java.io.File, webpackConfigTemplate: java.io.File, workingDirectory: java.io.File, depsOutput: java.io.File) = {

    val to = workingDirectory / "webpack.config.js"
    println("TO " + to.getAbsolutePath)

    val configFile = setConfigFile(entry, depsOutput, webpackConfigTemplate, to)
    //val configFile = workingDirectory / "scalajs.webpack.config.js"

    val webpackPath = workingDirectory / "node_modules/webpack/bin/webpack"

    val cmd = Seq(
      webpackPath.getAbsolutePath,
      "--profile",
      "--json",
      "--config",
      configFile.toFile.getAbsolutePath
    )

    External.run("node", cmd, workingDirectory)
  }

  def setConfigFile(entry: java.io.File, depsOutput: java.io.File, template: java.io.File, to: java.io.File) = {

    to.content =
      template.content
        .replace(
          "##openmoleJS##",
          entry.getAbsolutePath)
        .replace(
          "##bundleOutputDir##",
          depsOutput.getParentFile.getAbsolutePath)
        .replace(
          "##bundleName##",
          depsOutput.getName
        )

  }

  //  case class JSDep(name: String, version: String)
  //
  //  val scaladgetVersion = "1.9.0"
  //  val bootstrapNativeVersion = scaladgetVersion
  //  val highlightVersion = "10.4.1"
  //  val aceVersion = "1.4.3"
  //
  //  val jsDeps = Seq(
  //    JSDep("bootstrap.native", bootstrapNativeVersion),
  //    JSDep("highlight.js", highlightVersion),
  //    JSDep("ace-builds", aceVersion),
  //    JSDep("mode-scala")
  // )

  // For now, let's try static references to js lib and versions.

  //  val manifestFileName = "NPM_DEPENDENCIES"
  //
  //  type Dependencies = List[(String, String)]
  //
  //  case class NpmDependencies(
  //                              compileDependencies: Dependencies,
  //                              testDependencies: Dependencies,
  //                              compileDevDependencies: Dependencies,
  //                              testDevDependencies: Dependencies
  //                            ) {
  //    /** Merge operator */
  //    def ++ (that: NpmDependencies): NpmDependencies =
  //      NpmDependencies(
  //        compileDependencies ++ that.compileDependencies,
  //        testDependencies ++ that.testDependencies,
  //        compileDevDependencies ++ that.compileDevDependencies,
  //        testDevDependencies ++ that.testDevDependencies
  //      )
  //  }
  //
  //  def collectFromClasspath(cp: Def.Classpath): NpmDependencies =
  //    (
  //      for {
  //        cpEntry <- Attributed.data(cp) if cpEntry.exists
  //        results <-
  //          if (cpEntry.isFile && cpEntry.name.endsWith(".jar")) {
  //            val stream = new ZipInputStream(new BufferedInputStream(new FileInputStream(cpEntry)))
  //            try {
  //              Iterator.continually(stream.getNextEntry())
  //                .takeWhile(_ != null)
  //                .filter(_.getName == NpmDependencies.manifestFileName)
  //                .map(_ => Json.parse(IO.readStream(stream)).as[NpmDependencies])
  //                .to[Seq]
  //            } finally {
  //              stream.close()
  //            }
  //          } else if (cpEntry.isDirectory) {
  //            for {
  //              (file, _) <- Path.selectSubpaths(cpEntry, new ExactFilter(NpmDependencies.manifestFileName))
  //            } yield {
  //              Json.parse(IO.read(file)).as[NpmDependencies]
  //            }
  //          } else sys.error(s"Illegal classpath entry: ${cpEntry.absolutePath}")
  //      } yield results
  //      ).fold(NpmDependencies(Nil, Nil, Nil, Nil))(_ ++ _)
}