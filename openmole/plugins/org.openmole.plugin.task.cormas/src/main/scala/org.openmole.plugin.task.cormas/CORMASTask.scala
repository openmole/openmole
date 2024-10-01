package org.openmole.plugin.task.cormas

import monocle.Focus

import org.openmole.core.context.{ Context, Namespace }
import org.openmole.core.argument.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.setter._
import org.openmole.core.workflow.task.{ Task, TaskExecutionContext }
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.plugin.task.container.{ ContainerSystem, ContainerTask, DockerImage, HostFile }
import org.openmole.plugin.task.external._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.tool.outputredirection.OutputRedirection

import org.openmole.plugin.task.container
import org.openmole.plugin.task.container._
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object CORMASTask:

  given InputOutputBuilder[CORMASTask] = InputOutputBuilder(Focus[CORMASTask](_.config))
  given ExternalBuilder[CORMASTask] = ExternalBuilder(Focus[CORMASTask](_.external))
  given InfoBuilder[CORMASTask] = InfoBuilder(Focus[CORMASTask](_.info))
  given MappedInputOutputBuilder[CORMASTask] = MappedInputOutputBuilder(Focus[CORMASTask](_.mapped))

  def cormasImage(image: String, version: String) = DockerImage(image, version)

  def apply(
    script:               RunnableScript,
    forceUpdate:          Boolean                       = false,
    errorOnReturnValue:   Boolean                       = true,
    returnValue:          OptionalArgument[Val[Int]]    = None,
    stdOut:               OptionalArgument[Val[String]] = None,
    stdErr:               OptionalArgument[Val[String]] = None,
    environmentVariables: Vector[EnvironmentVariable]   = Vector.empty,
    hostFiles:            Vector[HostFile]              = Vector.empty,
    install:              Seq[String]                   = Seq.empty,
    clearContainerCache:    Boolean                     = false,
    version:                String                      = "latest",
    containerSystem:        ContainerSystem             = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, _workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService): CORMASTask =

    val preparedImage = ContainerTask.install(containerSystem, cormasImage("elcep/cormas", version), install = install, clearCache = clearContainerCache)

    new CORMASTask(
      preparedImage,
      script,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      stdOut = stdOut,
      stdErr = stdErr,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      mapped = MappedInputOutputConfig())


case class CORMASTask(
  image:                InstalledContainerImage,
  script:               RunnableScript,
  errorOnReturnValue:   Boolean,
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

  override protected def process(executionContext: TaskExecutionContext): FromContext[Context] = FromContext: p ⇒
    import p._
    import Mapped.noFile

    def inputJSONName = "input.json"
    def outputJSONName = "output.json"

    import org.openmole.core.json.*

    def inputsFields: Seq[JField] = noFile(mapped.inputs).map { i ⇒ i.name -> (toJSONValue(context(i.v)): JValue) }
    def inputDictionary = JObject(inputsFields: _*)

    def readOutputJSON(file: File) =
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      val outputValues = parse(file.content)
      val outputMap = outputValues.asInstanceOf[JObject].obj.toMap
      noFile(mapped.outputs).map: o ⇒
        jValueToVariable(
          outputMap.getOrElse(o.name, throw new UserBadDataError(s"Output named $name not found in the resulting json file ($outputJSONName) content is ${file.content}.")).asInstanceOf[JValue], 
          o.v,
          unwrapArrays = true)

    val outputFile = Val[File]("outputFile", Namespace("CormasTask"))
    val jsonInputs = executionContext.taskExecutionDirectory.newFile("inputs", ".json")
    val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".st")

    def scriptName = "_script_.st"
    def workDirectory = "/_workdirectory_"

    jsonInputs.content = compact(render(inputDictionary))
    scriptFile.content = RunnableScript.content(script)

    def containerTask =
      ContainerTask.internal(
        image = image,
        command = s"""/pharo --headless /Pharo.image eval ./$scriptName""",
        workDirectory = Some(workDirectory),
        errorOnReturnValue = errorOnReturnValue,
        returnValue = returnValue,
        hostFiles = hostFiles,
        environmentVariables = environmentVariables,
        stdOut = stdOut,
        stdErr = stdErr,
        config = InputOutputConfig(),
        external = external,
        info = info) set (
        resources += (jsonInputs, inputJSONName, true),
        resources += (scriptFile, scriptName, true),
        outputFiles += (outputJSONName, outputFile),
        Mapped.files(mapped.inputs).map { m ⇒ inputFiles += (m.v, m.name, true) },
        Mapped.files(mapped.outputs).map { m ⇒ outputFiles += (m.name, m.v) }
      )

    val resultContext = containerTask.process(executionContext).from(p.context)(p.random, p.tmpDirectory, p.fileService)
    resultContext ++ readOutputJSON(resultContext(outputFile))




