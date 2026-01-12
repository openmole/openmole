package org.openmole.gui.server.jscompile


import org.openmole.core.exception.InternalProcessingError
import org.openmole.tool.file.*
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.networkservice.*

import java.nio.file.Files

object EsBuild extends JavaLogger:

  def run(entry: java.io.File, workingDirectory: java.io.File, depsOutput: File)(using networkService: NetworkService) =

    val fromNodeModules = workingDirectory / "node_modules"
    val nodeModulesDirectory = depsOutput.getParentFile / "node_modules"

    if !nodeModulesDirectory.exists
    then fromNodeModules copy nodeModulesDirectory
    
    
    import scala.sys.process.{BasicIO, Process, ProcessLogger}
    import java.nio.file.attribute.PosixFilePermission._
    val nodeMajorVersion =
      Process("node --version").lazyLines.headOption match
        case Some(v) => v.dropWhile(_ == 'v').takeWhile(_.isDigit).toInt
        case None => throw new InternalProcessingError("""Error running "node --version" returned nothing""")

    val nodeEnv =
      if nodeMajorVersion >= 17
      then Seq("NODE_OPTIONS" -> "--openssl-legacy-provider")
      else Seq()

    val esbuildBin =
      workingDirectory / "node_modules/esbuild/bin/esbuild"
    
    esbuildBin.setExecutable(true)

    val cmd = Seq(
      entry.getAbsolutePath,
      "--bundle",
      "--format=esm",
      s"--outfile=${depsOutput.getAbsolutePath}"
    )

    External.run(
      esbuildBin.getAbsolutePath,
      cmd,
      workingDirectory,
      env = nodeEnv ++ NetworkService.proxyVariables
    )
