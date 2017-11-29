package org.openmole.plugin.task.r

import org.openmole.plugin.task.udocker._
import org.openmole.core.dsl._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.plugin.task.external._
import cats.implicits._
import monocle.macros._
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.dsl._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.threadprovider.ThreadProvider

object RTask {

  def installLibraries(libraries: Seq[String]): Vector[String] =
    if (libraries.isEmpty) Vector()
    else Vector(s"""R -e 'install.packages(c(${libraries.map(lib ⇒ '"' + s"$lib" + '"').mkString(",")}), dependencies = T)'""")

  def rImage(version: OptionalArgument[String]) = DockerImage("r-base", version.getOrElse("latest"))

  def apply(
    script:    File,
    arguments: OptionalArgument[String] = None,
    libraries: Seq[String]              = Seq.empty,
    version:   OptionalArgument[String] = None
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider) =
    UDockerTask(
      rImage(version),
      s"R --slave -f ${script.getName}" + arguments.map(a ⇒ s" --args ${a}").getOrElse(""),
      installCommands = installLibraries(libraries)
    ) set (
        resources += script,
        reuseContainer := true
      )

}

object RScriptTask {

  implicit def isTask: InputOutputBuilder[RScriptTask] = InputOutputBuilder(RScriptTask.config)
  implicit def isExternal: ExternalBuilder[RScriptTask] = ExternalBuilder(RScriptTask.external)

  // FIXME libraries not taken into account
  def apply(
    script:    FromContext[String],
    libraries: Seq[String]              = Seq.empty,
    version:   OptionalArgument[String] = None
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider): RScriptTask =
    RScriptTask(
      script,
      UDockerTask.toLocalImage(RTask.rImage(version)) match {
        case Right(x) ⇒ x
        case Left(x)  ⇒ throw new UserBadDataError(x.msg)
      },
      InputOutputConfig(),
      External()
    )

}

@Lenses case class RScriptTask(
  script:           FromContext[String],
  localDockerImage: LocalDockerImage,
  config:           InputOutputConfig,
  external:         External
) extends Task with ValidateTask {

  override def validate = Validate { p ⇒
    import p._
    val allInputs = External.PWD :: inputs.toList
    script.validate(inputs.toList) ++
      External.validate(external)(allInputs).apply
  }

  override def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import parameters._

    parameters.newFile.withTmpFile("script", ".R") { scriptFile ⇒
      scriptFile.content = script.from(context)

      val uDocker =
        UDockerArguments(
          localDockerImage,
          s"R --slave -f ${scriptFile.getName}"
        )

      val udockerTask = {
        import executionContext._

        UDockerTask.fromUDocker(uDocker) set (
          resources += scriptFile,
          reuseContainer := true
        )
      }

      udockerTask.process(executionContext).apply(context)
    }
  }

}