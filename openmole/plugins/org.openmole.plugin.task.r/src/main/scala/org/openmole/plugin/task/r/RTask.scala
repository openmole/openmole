package org.openmole.plugin.task.r

import org.openmole.plugin.task.udocker._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.plugin.task.external._
import org.openmole.core.expansion._
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.tool.hash._
import org.openmole.core.dsl._
import org.openmole.core.outputredirection.OutputRedirection

object RTask {

  sealed trait InstallCommand
  object InstallCommand {
    case class RLibrary(name: String) extends InstallCommand

    def toCommand(installCommands: InstallCommand) = {
      installCommands match {
        case RLibrary(name) ⇒
          //Vector(s"""R -e 'install.packages(c(${names.map(lib ⇒ '"' + s"$lib" + '"').mkString(",")}), dependencies = T)'""")
          s"""R -e 'install.packages(c("$name"), dependencies = T)'"""
      }
    }

    implicit def stringToRLibrary(name: String): InstallCommand = RLibrary(name)
    def installCommands(libraries: Vector[InstallCommand]): Vector[String] = libraries.map(InstallCommand.toCommand)
  }

  def rImage(version: OptionalArgument[String]) = DockerImage("r-base", version.getOrElse("latest"))

  def apply(
    script:      FromContext[String],
    install:     Seq[InstallCommand]      = Seq.empty,
    version:     OptionalArgument[String] = None,
    forceUpdate: Boolean                  = true
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection) = {
    val scriptVariable = Val[File]("script", org.openmole.core.context.Namespace("RTask"))

    val scriptContent = FromContext[File] { p ⇒
      val scriptFile = p.newFile.newFile("script", ".R")
      scriptFile.content = script.from(p.context)(p.random, p.newFile, p.fileService)
      p.fileService.deleteWhenGarbageCollected(scriptFile)
      scriptFile
    }

    val installCommands = InstallCommand.installCommands(install.toVector)
    val cacheKey: Option[String] = Some((Seq(rImage(version).image, rImage(version).tag) ++ installCommands).mkString("\n").hash().toString)

    UDockerTask(
      rImage(version),
      s"R --slave -f script.R",
      installCommands = installCommands,
      cachedKey = OptionalArgument(cacheKey),
      forceUpdate = forceUpdate
    ) set (
        inputFiles += (scriptVariable, "script.R", true),
        scriptVariable := scriptContent,
        reuseContainer := true
      )
  }

}
