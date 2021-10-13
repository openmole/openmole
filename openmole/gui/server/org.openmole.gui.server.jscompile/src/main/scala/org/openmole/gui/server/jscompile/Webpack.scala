package org.openmole.gui.server.jscompile

import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger

object Webpack extends JavaLogger {

  def run(entry: java.io.File, webpackConfigTemplate: java.io.File, workingDirectory: java.io.File, depsOutput: java.io.File) = {

    val to = workingDirectory / "webpack.config.js"

    val configFile = setConfigFile(entry, depsOutput, webpackConfigTemplate, to)

    println("-- CONFIG FILE " + configFile.toFile.content)

    val webpackPath = workingDirectory / "node_modules/webpack/bin/webpack"

    val cmd = Seq(
      webpackPath.getAbsolutePath,
      "--profile",
      "--json",
      "--config",
      configFile.toFile.getAbsolutePath
    )

    val nodeModulesDirectory = depsOutput.getParentFile / "node_modules"
    if (!nodeModulesDirectory.exists) workingDirectory / "node_modules" copy nodeModulesDirectory

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