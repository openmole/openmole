package org.openmole.gui.server.jscompile

import org.openmole.core.exception.InternalProcessingError
import org.openmole.tool.file.*
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.networkservice.*

object Webpack extends JavaLogger:

  case class ExtraModule(jsFile: java.io.File, nodeModuleSubdirectory: String)

  def run(entry: java.io.File, webpackConfigTemplate: java.io.File, workingDirectory: java.io.File, depsOutput: java.io.File, extraModules: Seq[ExtraModule])(using networkService: NetworkService) = {

    val to = workingDirectory / "webpack.config.js"

    val configFile = setConfigFile(entry, depsOutput, webpackConfigTemplate, to)

    val webpackPath = workingDirectory / "node_modules/webpack/bin/webpack"

    val cmd = Seq(
      webpackPath.getAbsolutePath,
      "--progress",
      "--json",
      "--config",
      configFile.toFile.getAbsolutePath
    )

    val fromNodeModules = workingDirectory / "node_modules"
    val nodeModulesDirectory = depsOutput.getParentFile / "node_modules"

    if (!nodeModulesDirectory.exists) {
      fromNodeModules copy nodeModulesDirectory
      extraModules.foreach { em => em.jsFile copy nodeModulesDirectory / em.nodeModuleSubdirectory / em.jsFile.getName }
    }

    import scala.sys.process.{BasicIO, Process, ProcessLogger}
    val nodeMajorVersion =
      Process("node --version").lazyLines.headOption match
        case Some(v) => v.dropWhile(_ == 'v').takeWhile(_.isDigit).toInt
        case None => throw new InternalProcessingError("""Error running "node --version" returned nothing""")

    val nodeEnv =
      if nodeMajorVersion >= 17
      then Seq("NODE_OPTIONS" -> "--openssl-legacy-provider")
      else Seq()

    External.run("node", cmd, workingDirectory, env = nodeEnv ++ NetworkService.proxyVariables)
  }

  def setConfigFile(entry: java.io.File, depsOutput: java.io.File, template: java.io.File, to: java.io.File) =
    def escape(s: String) = s.replace("\\", "\\\\")

    to.content =
      template.content
        .replace(
          "##openmoleJS##",
          escape(entry.getAbsolutePath))
        .replace(
          "##webuiDir##",
          escape(entry.getParentFile.getAbsolutePath)
        )
        .replace(
          "##bundleOutputDir##",
          escape(depsOutput.getParentFile.getAbsolutePath))
        .replace(
          "##bundleName##",
          depsOutput.getName
        )
