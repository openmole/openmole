package org.openmole.plugin.task.r

import monocle.macros.Lenses
import org.openmole.plugin.task.udocker._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.plugin.task.external._
import org.openmole.core.expansion._
import org.openmole.core.threadprovider._
import org.openmole.tool.hash._
import org.openmole.core.dsl._
import org.openmole.core.outputredirection._
import org.openmole.core.workflow.builder._
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.systemexec._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.container

object RTask {

  implicit def isTask: InputOutputBuilder[RTask] = InputOutputBuilder(RTask._config)
  implicit def isExternal: ExternalBuilder[RTask] = ExternalBuilder(RTask.external)

  implicit def isBuilder = new ReturnValue[RTask] with ErrorOnReturnValue[RTask] with StdOutErr[RTask] with EnvironmentVariables[RTask] with HostFiles[RTask] with ReuseContainer[RTask] with WorkDirectory[RTask] with UDockerUser[RTask] { builder ⇒
    override def returnValue = RTask.returnValue
    override def errorOnReturnValue = RTask.errorOnReturnValue
    override def stdOut = RTask.stdOut
    override def stdErr = RTask.stdErr
    override def environmentVariables = RTask.uDocker composeLens UDockerArguments.environmentVariables
    override def hostFiles = RTask.uDocker composeLens UDockerArguments.hostFiles
    override def reuseContainer = RTask.uDocker composeLens UDockerArguments.reuseContainer
    override def workDirectory = RTask.uDocker composeLens UDockerArguments.workDirectory
    override def uDockerUser = RTask.uDocker composeLens UDockerArguments.uDockerUser
  }

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

  def rImage(version: String) = DockerImage("r-base", version)

  //jsonlite
  //toJSON(list(a, "tsrn", list("eui", "eui")), auto_unbox = TRUE)
  // e = fromJSON('[9,"tsrn",["eui","eui"]]')
  //i = e[[1]]

  //write(toJSON( iris ),'jstest')
  //res <- fromJSON( file="jstest")

  def apply(
    script:      FromContext[String],
    install:     Seq[InstallCommand] = Seq.empty,
    version:     String              = "3.4.3",
    forceUpdate: Boolean             = false
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection): RTask = {

    val installCommands = InstallCommand.installCommands(install.toVector ++ Seq(InstallCommand.RLibrary("jsonlite")))
    val cacheKey: Option[String] =
      Some((Seq(rImage(version).image, rImage(version).tag) ++ installCommands).mkString("\n").hash().toString)

    val uDockerArguments =
      UDockerTask.createUDocker(
        rImage(version),
        installCommands = installCommands,
        cachedKey = OptionalArgument(cacheKey),
        forceUpdate = forceUpdate
      )

    RTask(
      script = script,
      uDockerArguments,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      _config = InputOutputConfig(),
      external = External(),
      rInputs = Vector.empty,
      rOutputs = Vector.empty
    )
  }

}

@Lenses case class RTask(
  script:             FromContext[String],
  uDocker:            UDockerArguments,
  errorOnReturnValue: Boolean,
  returnValue:        Option[Val[Int]],
  stdOut:             Option[Val[String]],
  stdErr:             Option[Val[String]],
  _config:            InputOutputConfig,
  external:           External,
  rInputs:            Vector[(Val[_], String)], rOutputs: Vector[(String, Val[_])]) extends Task with ValidateTask {

  override def config = UDockerTask.config(_config, returnValue, stdOut, stdErr)
  override def validate = container.validateContainer(Vector(), uDocker.environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    newFile.withTmpFile("script", ".R") { scriptFile ⇒
      scriptFile.content = script.from(p.context)(p.random, p.newFile, p.fileService)

      def uDockerTask = UDockerTask(uDocker, s"R --slave -f script.R", errorOnReturnValue, returnValue, stdOut, stdErr, _config, external) set (
        resources += (scriptFile, "script.R", true),
        reuseContainer := true
      )

      rInputs.map { case (v, _) ⇒ context(v) }
      uDockerTask.process(executionContext).from(context)
    }
  }
}