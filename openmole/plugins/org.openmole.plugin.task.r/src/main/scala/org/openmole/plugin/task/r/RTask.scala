package org.openmole.plugin.task.r

import org.openmole.plugin.task.udocker._
import org.openmole.core.dsl._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.plugin.task.external._
import cats.implicits._
import org.openmole.core.expansion._

object RTask {

  def installLibraries(libraries: Seq[String]) = s"""R -e "install.packages(c(${libraries.map(lib ⇒ s"'$lib'").mkString(",")}), dependencies = T)""""

  def apply(
    script:    File,
    arguments: OptionalArgument[String] = None,
    libraries: Seq[String]              = Seq.empty,
    version:   OptionalArgument[String] = None
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService) = {

    UDockerTask(
      DockerImage("r-base", version.getOrElse("latest")),
      s"R --slave -f ${script.getName}" + arguments.map(a ⇒ " --args ${a}").getOrElse(""),
      installCommands = Vector(installLibraries(libraries))
    ) set (
        resources += script,
        reuseContainer := true
      )
  }

}

//object RScriptTask {
//
//  def apply(
//    script:    FromContext[String],
//    libraries: Seq[String]              = Seq.empty,
//    version:   OptionalArgument[String] = None
//  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService) =
//    UDockerTask(
//      DockerImage("r-base", version.getOrElse("latest")),
//      script.map(s ⇒ s"""R -e "$s""""),
//      installCommands = Vector(RTask.installLibraries(libraries))
//    ) set (
//        reuseContainer := true
//      )
//
//}
