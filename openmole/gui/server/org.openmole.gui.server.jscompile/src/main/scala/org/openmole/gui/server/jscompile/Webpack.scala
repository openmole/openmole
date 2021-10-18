package org.openmole.gui.server.jscompile

import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger

object Webpack extends JavaLogger {

  case class ExtraModule(jsFile: java.io.File, nodeModuleSubdirectory: String)

  def run(entry: java.io.File, webpackConfigTemplate: java.io.File, workingDirectory: java.io.File, depsOutput: java.io.File, extraModules: Seq[ExtraModule]) = {

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
      extraModules.foreach { em ⇒ em.jsFile copy nodeModulesDirectory / em.nodeModuleSubdirectory / em.jsFile.getName }
    }

    External.run("node", cmd, workingDirectory)
  }

  def setConfigFile(entry: java.io.File, depsOutput: java.io.File, template: java.io.File, to: java.io.File) = {

    to.content =
      template.content
        .replace(
          "##openmoleJS##",
          entry.getAbsolutePath)
        .replace(
          "##webuiDir##",
          entry.getParentFile.getAbsolutePath
        )
        .replace(
          "##bundleOutputDir##",
          depsOutput.getParentFile.getAbsolutePath)
        .replace(
          "##bundleName##",
          depsOutput.getName
        )

  }
}