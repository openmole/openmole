package org.openmole.plugin.task.r

import monocle.Focus
import org.openmole.core.context.{Namespace, Val}
import org.openmole.core.dsl._
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.argument.{OptionalArgument, *}
import org.openmole.core.fileservice._
import org.openmole.core.networkservice._
import org.openmole.core.preference._
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider._
import org.openmole.core.setter._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace._
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container.ContainerTask.install
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.core.json.*
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.core.dsl.extension._

import org.json4s.jackson.JsonMethods._
import org.json4s._

object RTask:

  given InputOutputBuilder[RTask] = InputOutputBuilder(Focus[RTask](_.config))
  given ExternalBuilder[RTask] = ExternalBuilder(Focus[RTask](_.external))
  given InfoBuilder[RTask] = InfoBuilder(Focus[RTask](_.info))
  given MappedInputOutputBuilder[RTask] = MappedInputOutputBuilder(Focus[RTask](_.mapped))

  case class RLibrary(name: String, version: Option[String] = None, dependencies: Boolean = false)

  object RLibrary:

    def toCommand(installCommand: RLibrary) =
      def dependencies(d: Boolean) = if(d) "T" else "F"

      installCommand match
        case RLibrary(name, None, d) ⇒
          s"""R --slave -e 'install.packages(c("$name"), dependencies = ${dependencies(d)}); library("$name")'"""
        case RLibrary(name, Some(version), d) ⇒
          s"""R --slave -e 'library(remotes); remotes::install_version("$name",version = "$version", dependencies = ${dependencies(d)}); library("$name")'"""

    def toCommandNoVersion(libraries: Seq[String], dependencies: Boolean): String =
      def list = libraries.map(l => s"\"$l\"").mkString(",")
      def dep = if dependencies then "T" else "F"
      def load = libraries.map(l => s"""library("$l")""")
      def command = Seq(s"install.packages(c($list), dependencies = $dep)") ++ load

      s"""R --slave -e '${command.mkString("; ")}'"""

    given Conversion[String, RLibrary] = name => RLibrary(name, None)
    given Conversion[(String, String), RLibrary] = (name, version) => RLibrary(name, Some(version))
    given Conversion [(String, String, Boolean), RLibrary] = (name, version, dep) => RLibrary(name, Some(version), dep)
    given Conversion [(String, Boolean), RLibrary] = (name, dep) => RLibrary(name, None, dep)

    def installCommands(libraries: Vector[RLibrary]): Seq[String] =
      val (noVersion, withVersion) = libraries.partition(l => l.version.isEmpty)
      val (noVersionNoDep, noVersionWithDep) = noVersion.partition(l => !l.dependencies)

      val update = if libraries.nonEmpty then Seq("rm -rf /var/lib/apt/lists", "apt update") else Seq()

      update ++
        (if noVersionNoDep.nonEmpty then Seq(toCommandNoVersion(noVersionNoDep.map(_.name), false)) else Seq()) ++
        (if noVersionWithDep.nonEmpty then Seq(toCommandNoVersion(noVersionWithDep.map(_.name), true)) else Seq()) ++
        withVersion.map(RLibrary.toCommand)


  def rImage(image: String, version: String) = DockerImage(image, version)

  def apply(
    script:                     RunnableScript,
    install:                    Seq[String]                        = Seq.empty,
    libraries:                  Seq[RLibrary]                      = Seq.empty,
    prepare:                    Seq[String]                        = Seq.empty,
    image:                      String                             = "openmole/r2u:4.3.0",
    errorOnReturnValue:         Boolean                            = true,
    returnValue:                OptionalArgument[Val[Int]]         = None,
    stdOut:                     OptionalArgument[Val[String]]      = None,
    stdErr:                     OptionalArgument[Val[String]]      = None,
    hostFiles:                  Seq[HostFile]                      = Vector.empty,
    environmentVariables:       Seq[EnvironmentVariable]           = Vector.empty,
    clearContainerCache:        Boolean                            = false,
    containerSystem:            ContainerSystem                    = ContainerSystem.default
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): RTask =

    RTask(
      script = script,
      image = image,
      install = install,
      libraries = libraries,
      containerSystem = containerSystem,
      clearContainerCache = clearContainerCache,
      prepare = prepare,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      stdOut = stdOut,
      stdErr = stdErr,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      mapped = MappedInputOutputConfig()
    ) set (outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)


case class RTask(
  script:               RunnableScript,
  image:                String,
  install:              Seq[String],
  libraries:            Seq[RTask.RLibrary],
  containerSystem:      ContainerSystem,
  clearContainerCache:  Boolean,
  errorOnReturnValue:   Boolean,
  prepare:              Seq[String],
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  hostFiles:            Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  config:               InputOutputConfig,
  external:             External,
  info:                 InfoConfig,
  mapped:               MappedInputOutputConfig) extends Task with ValidateTask:

  override def validate = container.validateContainer(Vector(), environmentVariables, external)

  def apply(taskBuildContext: TaskExecutionBuildContext): TaskExecution =
    def workDirectory = "/_workdirectory_"
    def inputArrayName = "generatedomarray"
    def rScriptPath = s"$workDirectory/_generatedomscript_.R"
    def inputJSONPath = s"$workDirectory/_generatedominputs_.json"
    def outputJSONPath = s"$workDirectory/_generatedomoutputs_.json"

    def installCommands = install ++ RTask.RLibrary.installCommands(libraries.toVector)

    val containerTaskExecution =
      import taskBuildContext.*

      ContainerTask.execution(
        image = ContainerTask.install(containerSystem, image, installCommands, clearCache = clearContainerCache),
        command = prepare ++ Seq(s"R --slave -f $rScriptPath"),
        workDirectory = Some(workDirectory),
        errorOnReturnValue = errorOnReturnValue,
        returnValue = returnValue,
        hostFiles = hostFiles,
        environmentVariables = environmentVariables,
        stdOut = stdOut,
        stdErr = stdErr,
        config = config,
        external = external,
        info = info)(taskBuildContext)

    TaskExecution: p =>
      import p.*
      import Mapped.noFile
      import org.json4s.jackson.JsonMethods.*

      def writeInputsJSON(inputs: Vector[ Mapped[?]], file: File) =
        def values = inputs.map { m ⇒ m.v.`type`.manifest.array(context(m.v)) }
        file.content = compact(render(toJSONValue(values.toArray[Any])))

      def rInputMapping(inputs: Vector[ Mapped[?]], arrayName: String) =
        inputs.zipWithIndex.map { (m, i) ⇒ s"${m.name} = $arrayName[[${i + 1}]][[1]]" }.mkString("\n")

      def rOutputMapping =
        s"""list(${noFile(mapped.outputs).map { _.name }.mkString(",")})"""

      def readOutputJSON(file: File) =
        import org.json4s._
        import org.json4s.jackson.JsonMethods._
        val outputValues = parse(file.content)
        (outputValues.asInstanceOf[JArray].arr zip noFile(mapped.outputs).map(_.v)).map { (jvalue, v) ⇒ jValueToVariable(jvalue, v, unwrapArrays = true) }

      val jsonInputs = executionContext.taskExecutionDirectory.newFile("input", ".json")
      val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".R")

      val valueMappedInputs = noFile(mapped.inputs)
      writeInputsJSON(valueMappedInputs, jsonInputs)

      scriptFile.content = s"""
        |library("jsonlite")
        |$inputArrayName = fromJSON("$inputJSONPath", simplifyMatrix = FALSE)
        |${rInputMapping(valueMappedInputs, inputArrayName)}
        |${RunnableScript.content(script)}
        |write_json($rOutputMapping, "$outputJSONPath", always_decimal = TRUE)
        """.stripMargin

      val outputFile = Val[File]("outputFile", Namespace("RTask"))

      def containerTask =
        containerTaskExecution.set (
          resources += (scriptFile, rScriptPath),
          resources += (jsonInputs, inputJSONPath),
          outputFiles += (outputJSONPath, outputFile),
          Mapped.files(mapped.inputs).map { m ⇒ inputFiles += (m.v, m.name, true) },
          Mapped.files(mapped.outputs).map { m ⇒ outputFiles += (m.name, m.v) }
        )

      val resultContext =
        try containerTask(executionContext).from(context)
        catch
          case t: UserBadDataError => throw UserBadDataError(s"Error executing script:\n${scriptFile.content}", t)
          case t: Throwable => throw InternalProcessingError(s"Error executing script:\n${scriptFile.content}", t)

      resultContext ++ readOutputJSON(resultContext(outputFile))

