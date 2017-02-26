package org.openmole.plugin.task.udocker

import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import monocle.macros._
import java.io.File

import org.openmole.core.context._
import org.openmole.core.workflow.builder._
import org.openmole.tool.random._
import org.openmole.tool.stream._
import org.openmole.tool.file._
import org.openmole.core.expansion._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec._
import cats.implicits._

import org.json4s._
import org.json4s.jackson.JsonMethods._

import org.openmole.core.exception._

object UDockerTask {
  implicit def isTask: InputOutputBuilder[UDockerTask] = InputOutputBuilder(UDockerTask._config)
  implicit def isExternal: ExternalBuilder[UDockerTask] = ExternalBuilder(UDockerTask.external)

  implicit def isBuilder = new ReturnValue[UDockerTask] with ErrorOnReturnValue[UDockerTask] with StdOutErr[UDockerTask] with EnvironmentVariables[UDockerTask] with WorkDirectory[UDockerTask] with HostFiles[UDockerTask] { builder ⇒

    override def environmentVariables = UDockerTask.environmentVariables
    override def workDirectory = UDockerTask.workDirectory
    override def returnValue = UDockerTask.returnValue
    override def errorOnReturnValue = UDockerTask.errorOnReturnValue
    override def stdOut = UDockerTask.stdOut
    override def stdErr = UDockerTask.stdErr
    override def hostFiles = UDockerTask.hostFiles
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
      workDirectory = None,
      hostFiles = Vector.empty,
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
    workDirectory:        Option[String],
    hostFiles:            Vector[(String, Option[String])],
    _config:              InputOutputConfig,
    external:             External
) extends Task with ValidateTask {

  override protected def process(ctx: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = External.withWorkDir(executionContext) { taskWorkDirectory ⇒

    taskWorkDirectory.mkdirs()

    val context = ctx + (External.PWD → taskWorkDirectory.getAbsolutePath)

    def subDirectory(name: String) = {
      val dir = taskWorkDirectory / name
      dir.mkdirs()
      dir
    }

    val udocker = {
      val destination = taskWorkDirectory / "udocker"
      this.getClass.getClassLoader.getResourceAsStream("udocker").copy(destination)
      destination.setExecutable(true)
      destination
    }

    val containers = subDirectory("containers")

    def udockerVariables =
      Vector(
        "HOME" → taskWorkDirectory.getAbsolutePath,
        "UDOCKER_DIR" → (taskWorkDirectory / ".udocker").getAbsolutePath,
        "UDOCKER_TMPDIR" → subDirectory("tmpdir").getAbsolutePath,
        //"UDOCKER_REPOS" → subDirectory("repo").getAbsolutePath,
        //"UDOCKER_LAYERS" → subDirectory("layers").getAbsolutePath,
        "UDOCKER_CONTAINERS" → containers.getAbsolutePath
      )

    val imageId = {
      val commandline = commandLine(s"${udocker.getAbsolutePath} load -i ${archive.getAbsolutePath}", taskWorkDirectory.getAbsolutePath, context)
      val result = execute(commandline, taskWorkDirectory, udockerVariables, returnOutput = true, returnError = true)
      val output = result.output.get
      output.lines.toSeq.last
    }

    val inputsFiles = {
      val dir = taskWorkDirectory / "inputFiles"
      dir.mkdirs()
      dir
    }

    val dockerWorkDirectory = {
      val commandline = commandLine(s"${udocker.getAbsolutePath} inspect $imageId", taskWorkDirectory.getAbsolutePath, context)
      val result = execute(commandline, taskWorkDirectory, udockerVariables, returnOutput = true, returnError = true)
      implicit def format = DefaultFormats
      result.output.flatMap { o ⇒ (parse(o) \ "config" \ "WorkingDir").extractOpt[String] }
    }

    val actualWorkDirectory = workDirectory orElse dockerWorkDirectory getOrElse "/"

    val inputDirectory = taskWorkDirectory / "inputs"

    def inputPathResolver(path: String) = {
      if (new File(path).isAbsolute) inputDirectory / path
      else inputDirectory / actualWorkDirectory / path
    }

    val (preparedContext, preparedFilesInfo) = external.prepareAndListInputFiles(context, inputPathResolver)

    def volumes =
      preparedFilesInfo.map { case (f, d) ⇒ d.getAbsolutePath → f.name } ++
        hostFiles.map { case (f, b) ⇒ f → b.getOrElse(f) }

    def runCommand: FromContext[String] = {
      val variablesArgument = environmentVariables.map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")
      def volumesArgument = volumes.map { case (host, container) ⇒ s"""-v "$host":"$container"""" }.mkString(" ")
      command.map(cmd ⇒ s"${udocker.getAbsolutePath} run $variablesArgument $volumesArgument ${imageId} $cmd")
    }

    val executionResult = executeAll(
      taskWorkDirectory,
      udockerVariables,
      errorOnReturnValue,
      returnValue,
      stdOut,
      stdErr,
      context,
      List(runCommand)
    )

    // FIXME use name when run --name is working
    val rootDirectory = containers.listFiles().head / "ROOT"

    def outputPathResolver(filePath: String): File = {
      def isAbsolute = new File(filePath).isAbsolute
      if (isAbsolute) rootDirectory / filePath else rootDirectory / actualWorkDirectory / filePath
    }

    val retContext = external.fetchOutputFiles(preparedContext, outputPathResolver)
    external.checkAndClean(this, retContext, taskWorkDirectory)

    retContext ++
      List(
        stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
        stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
        returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
      ).flatten
  }

  override def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)

  override def validate: Seq[Throwable] = {
    val allInputs = External.PWD :: inputs.toList

    def validateArchive = if (!archive.exists) Seq(new UserBadDataError(s"Cannot find specified Archive $archive in your work directory. Did you prefix the path with `workDirectory / `?")) else Seq.empty
    def validateVariables = environmentVariables.map(_._2).flatMap(_.validate(allInputs))
    command.validate(allInputs) ++ validateArchive ++ validateVariables ++ External.validate(external, allInputs)
  }

}
