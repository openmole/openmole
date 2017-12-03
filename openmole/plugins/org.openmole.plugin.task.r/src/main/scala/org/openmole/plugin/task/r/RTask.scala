package org.openmole.plugin.task.r

import org.openmole.plugin.task.udocker._
import org.openmole.core.dsl._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.plugin.task.external._
import cats.implicits._
import monocle.macros._
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.dsl._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.threadprovider.ThreadProvider

object RScriptTask {

  def apply(
    script:    File,
    arguments: OptionalArgument[String] = None,
    libraries: Seq[String]              = Seq.empty,
    version:   OptionalArgument[String] = None
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider) =
    UDockerTask(
      RTask.rImage(version),
      s"R --slave -f ${script.getName}" + arguments.map(a ⇒ s" --args ${a}").getOrElse(""),
      installCommands = RTask.installLibraries(libraries)
    ) set (
        resources += script,
        reuseContainer := true
      )

}

object RTask {

  def installLibraries(libraries: Seq[String]): Vector[String] =
    if (libraries.isEmpty) Vector()
    else Vector(s"""R -e 'install.packages(c(${libraries.map(lib ⇒ '"' + s"$lib" + '"').mkString(",")}), dependencies = T)'""")

  def rImage(version: OptionalArgument[String]) = DockerImage("r-base", version.getOrElse("latest"))

  def apply(
    script:    FromContext[String],
    libraries: Seq[String]              = Seq.empty,
    version:   OptionalArgument[String] = None
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider) = {
    val scriptVariable = Val[File]("script", org.openmole.core.context.Namespace("RTask"))

    val scriptContent = FromContext[File] { p ⇒
      val scriptFile = p.newFile.newFile("script", ".R")
      scriptFile.content = script.from(p.context)(p.random, p.newFile, p.fileService)
      p.fileService.deleteWhenGarbageCollected(scriptFile)
      scriptFile
    }

    UDockerTask(
      rImage(version),
      s"R --slave -f script.R",
      installCommands = installLibraries(libraries)
    ) set (
        inputFiles += (scriptVariable, "script.R", true),
        scriptVariable := scriptContent,
        reuseContainer := true
      )
  }

}
