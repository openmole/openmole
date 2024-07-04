
package org.openmole.plugin.task.julia

import monocle.Focus
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.FileService
import org.openmole.core.argument.OptionalArgument
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.setter.*
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container.*
import org.openmole.plugin.task.external.*
import org.openmole.core.json.*
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.plugin.task.container

/**
 * https://docs.julialang.org/en/v1/
 */
object JuliaTask:

  given InputOutputBuilder[JuliaTask] = InputOutputBuilder(Focus[JuliaTask](_.config))
  given ExternalBuilder[JuliaTask] = ExternalBuilder(Focus[JuliaTask](_.external))
  given InfoBuilder[JuliaTask] = InfoBuilder(Focus[JuliaTask](_.info))
  given MappedInputOutputBuilder[JuliaTask] = MappedInputOutputBuilder(Focus[JuliaTask](_.mapped))

  def installCommands(install: Seq[String], libraries: Seq[String]): Vector[String] =
     (install ++ Seq("""julia -e 'using Pkg; Pkg.add.([ """ + libraries.map { l ⇒ "\""+l+"\"" }.mkString(",")+"""])'""" )).toVector

  def apply(
    script:                 RunnableScript,
    arguments:              OptionalArgument[String] = None,
    libraries:              Seq[String]                        = Seq.empty,
    install:                Seq[String]                        = Seq.empty,
    prepare:                Seq[String]                        = Seq.empty,
    version:                String                             = "1.6.7",
    hostFiles:              Seq[HostFile]                      = Vector.empty,
    environmentVariables:   Seq[EnvironmentVariable]           = Vector.empty,
    errorOnReturnValue:     Boolean                            = true,
    returnValue:            OptionalArgument[Val[Int]]         = None,
    stdOut:                 OptionalArgument[Val[String]]      = None,
    stdErr:                 OptionalArgument[Val[String]]      = None,
    containerSystem:        ContainerSystem                    = ContainerSystem.default,
    installContainerSystem: ContainerSystem                    = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService) =

  new JuliaTask(
    script = script,
    arguments = arguments.option,
    image = ContainerTask.install(installContainerSystem, DockerImage("julia", version), installCommands(install, Seq("JSON")++libraries)),
    prepare = prepare,
    errorOnReturnValue = errorOnReturnValue,
    returnValue = returnValue,
    stdOut = stdOut,
    stdErr = stdErr,
    hostFiles = hostFiles,
    environmentVariables = environmentVariables,
    containerSystem = containerSystem,
    config = InputOutputConfig(),
    external = External(),
    info = InfoConfig(),
    mapped = MappedInputOutputConfig()
    ) set (outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)



case class JuliaTask(
  script:                 RunnableScript,
  image:                  InstalledImage,
  arguments:              Option[String],
  prepare:                Seq[String],
  errorOnReturnValue:     Boolean,
  returnValue:            Option[Val[Int]],
  stdOut:                 Option[Val[String]],
  stdErr:                 Option[Val[String]],
  hostFiles:              Seq[HostFile],
  environmentVariables:   Seq[EnvironmentVariable],
  containerSystem:        ContainerSystem,
  config:                 InputOutputConfig,
  external:               External,
  info:                   InfoConfig,
  mapped:                 MappedInputOutputConfig) extends Task with ValidateTask:

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate = container.validateContainer(Vector(), environmentVariables, external)

  override def process(executionContext: TaskExecutionContext) = FromContext: p ⇒
    import org.json4s.jackson.JsonMethods._
    import p._
    import Mapped.noFile

    def writeInputsJSON(file: File): Unit =
      def values = noFile(mapped.inputs).map { m => (m.name,p.context(m.v)) }
      file.content = "{" + values.map{ (name,value) => "\""+name+"\": "+compact(render(toJSONValue(value)))}.mkString(",") + "}"

    def readOutputJSON(file: File) =
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      val outputValues = parse(file.content)
      (outputValues.asInstanceOf[JArray].arr zip noFile(mapped.outputs).map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v, unwrapArrays = true) }

    def inputMapping(dicoName: String): String =
      noFile(mapped.inputs).zipWithIndex.map {
        (m, i) ⇒ s"${m.name} = $dicoName[\"${m.name}\"]"
      }.mkString("\n")

    def outputMapping: String =
      s"""[${noFile(mapped.outputs).map { m ⇒ m.name }.mkString(",")}]"""

    def workDirectory = "/_workdirectory_"

    val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".jl")
    val jsonInputs = executionContext.taskExecutionDirectory.newFile("inputs", ".json")

    val resultContext: Context =
      def inputArrayName = "_generateddata_"
      def scriptName = s"$workDirectory/_generatescript_.jl"
      def inputJSONName = s"$workDirectory/_inputs_.json"
      def outputJSONName = s"$workDirectory/_outputs_.json"

      writeInputsJSON(jsonInputs)

      def scriptContent =
        s"""
           |import JSON
           |$inputArrayName = "$inputJSONName" |> open |> JSON.parse
           |${inputMapping(inputArrayName)}
           |${RunnableScript.content(script)}
           |write(open("$outputJSONName","w"), JSON.json($outputMapping))
          """.stripMargin

      scriptFile.content = scriptContent


      val outputFile = Val[File]("outputFile", Namespace("JuliaTask"))

      val argumentsValue = arguments.map(" " + _).getOrElse("")

      def containerTask =
        ContainerTask.isolatedWorkdirectory(executionContext)(
          containerSystem = containerSystem,
          image = image,
          command = prepare ++ Seq(s"julia $scriptName $argumentsValue"),
          workDirectory = workDirectory,
          errorOnReturnValue = errorOnReturnValue,
          returnValue = returnValue,
          hostFiles = hostFiles,
          environmentVariables = environmentVariables,
          stdOut = stdOut,
          stdErr = stdErr,
          config = InputOutputConfig(),
          external = external,
          info = info,
          containerPoolKey = containerPoolKey) set (
            resources += (scriptFile, scriptName, true),
            resources += (jsonInputs, inputJSONName, true),
            outputFiles += (outputJSONName, outputFile),
            Mapped.files(mapped.inputs).map { m ⇒ inputFiles += (m.v, m.name, true) },
            Mapped.files(mapped.outputs).map { m ⇒ outputFiles += (m.name, m.v) }
          )

      val resultContext =
        try containerTask.process(executionContext).from(p.context)(p.random, p.tmpDirectory, p.fileService)
        catch
          case e: Throwable => throw InternalProcessingError(s"Script content was: $scriptContent", e)

      resultContext ++ readOutputJSON(resultContext(outputFile))

    resultContext





