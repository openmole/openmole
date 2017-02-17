package org.openmole.plugin.task.udocker

import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import monocle.macros._
import java.io.File
import java.util.UUID

import org.openmole.core.context._
import org.openmole.core.workflow.builder._
import org.openmole.tool.random._
import org.openmole.tool.stream._
import org.openmole.tool.file._
import org.openmole.core.expansion._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec._
import cats.implicits._
import monocle.Lens
import org.openmole.core.exception.UserBadDataError

object UDockerTask {
  implicit def isTask: InputOutputBuilder[UDockerTask] = InputOutputBuilder(UDockerTask._config)
  implicit def isExternal: ExternalBuilder[UDockerTask] = ExternalBuilder(UDockerTask.external)

  trait UDockerTaskBuilder[T] extends ReturnValue[T]
      with ErrorOnReturnValue[T]
      with StdOutErr[T]
      with EnvironmentVariables[T] { builder ⇒

    //def hostFiles: Lens[T, Vector[(String, Option[String])]]

  }

  implicit def isBuilder = new UDockerTaskBuilder[UDockerTask] {
    //override def hostFiles = CARETask.hostFiles
    override def environmentVariables = UDockerTask.environmentVariables
    //override def workDirectory = CARETask.workDirectory
    override def returnValue = UDockerTask.returnValue
    override def errorOnReturnValue = UDockerTask.errorOnReturnValue
    override def stdOut = UDockerTask.stdOut
    override def stdErr = UDockerTask.stdErr
  }

  def apply(
    archive: File,
    command: FromContext[String]
  )(implicit name: sourcecode.Name): UDockerTask =
    new UDockerTask(
      archive = archive,
      command = command,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      environmentVariables = Vector.empty,
      _config = InputOutputConfig(),
      external = External()
    )

}

@Lenses case class UDockerTask(
    archive:              File,
    command:              FromContext[String],
    errorOnReturnValue:   Boolean,
    returnValue:          Option[Val[Int]],
    stdOut:               Option[Val[String]],
    stdErr:               Option[Val[String]],
    environmentVariables: Vector[(String, FromContext[String])],
    _config:              InputOutputConfig,
    external:             External
) extends Task with ValidateTask {

  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = External.withWorkDir(executionContext) { taskWorkDirectory ⇒

    taskWorkDirectory.mkdirs()

    val udocker = {
      val destination = taskWorkDirectory / "udocker"
      this.getClass.getClassLoader.getResourceAsStream("udocker").copy(destination)
      destination.setExecutable(true)
      destination
    }

    // FIXME get rid of code to circonvent bug in udocker
    val archiveCopy = {
      val destination = taskWorkDirectory / "archive.tar"
      archive.copy(destination)
      destination
    }

    val imageId = UUID.randomUUID().toString

    def importCommand: FromContext[String] = s"./udocker import ${archiveCopy.getAbsolutePath} $imageId"
    def runCommand: FromContext[String] = {
      val varibleOptions = environmentVariables.map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")
      command.map(cmd ⇒ s"./udocker run $varibleOptions ${imageId} $cmd")
    }

    val udockerTmpDir = {
      val dir = taskWorkDirectory / "tmpdir"
      dir.mkdirs()
      dir
    }

    def udockerVariables =
      Vector(
        "UDOCKER_DIR" → (taskWorkDirectory / ".udocker").getAbsolutePath,
        "UDOCKER_TMPDIR" → udockerTmpDir.getAbsolutePath
      )

    // TODO pass env variables with -e
    val executionResult = executeAll(
      taskWorkDirectory,
      udockerVariables,
      errorOnReturnValue,
      returnValue,
      stdOut,
      stdErr,
      context,
      List(importCommand, runCommand)
    )

    context
  }

  override def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)

  override def validate: Seq[Throwable] = {
    def validateArchive = if (!archive.exists) Seq(new UserBadDataError(s"Cannot find specified Archive $archive in your work directory. Did you prefix the path with `workDirectory / `?")) else Seq.empty
    def validateVariables = environmentVariables.map(_._2).flatMap(_.validate(inputs.toList))
    command.validate(inputs.toList) ++ validateArchive ++ validateVariables
  }

}
