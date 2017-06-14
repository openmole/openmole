package org.openmole.plugin.task.r

import org.openmole.plugin.task.udocker._
import org.openmole.core.dsl._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.plugin.task.external._

object RTask {

  def apply(
    script:    File,
    arguments: OptionalArgument[String] = None,
    libraries: Seq[String]              = Seq.empty,
    version:   OptionalArgument[String] = None
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService) = {

    def installLibraries = s"""R -e "install.packages(c(${libraries.map(lib ⇒ s"'$lib'").mkString(",")}), dependencies = T)""""

    UDockerTask(
      DockerImage("r-base", version.getOrElse("latest")),
      s"R --slave -f ${script.getName}" + arguments.map(a ⇒ " --args ${a}").getOrElse(""),
      installCommands = Vector(installLibraries)
    ) set (
        resources += script,
        reuseContainer := true
      )
  }

}
