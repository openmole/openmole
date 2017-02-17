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

object UDockerTask {
  implicit def isTask: InputOutputBuilder[UDockerTask] = InputOutputBuilder(UDockerTask.config)
  //implicit def isExternal: ExternalBuilder[UDockerTask] = ExternalBuilder(UDockerTask.external)

  def apply(
    archive: File,
    command: FromContext[String],
    config:  InputOutputConfig   = InputOutputConfig()
  )(implicit name: sourcecode.Name): UDockerTask =
    new UDockerTask(
      archive = archive,
      command = command,
      config = config,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      environmentVariables = Vector.empty
    )

}

@Lenses case class UDockerTask(
    archive:              File,
    command:              FromContext[String],
    errorOnReturnValue:   Boolean,
    returnValue:          Option[Val[Int]],
    stdOut:               Option[Val[String]],
    stdErr:               Option[Val[String]],
    environmentVariables: Vector[(Val[_], String)],
    config:               InputOutputConfig
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
    def runCommand: FromContext[String] = command.map(cmd ⇒ s"./udocker run ${imageId} $cmd")

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
      environmentVariables.map { case (variable, name) ⇒ (name, context(variable).toString) } ++ udockerVariables,
      errorOnReturnValue,
      returnValue,
      stdOut,
      stdErr,
      context,
      List(importCommand, runCommand)
    )

    context
  }

  override def validate: Seq[Throwable] = Seq.empty
}
