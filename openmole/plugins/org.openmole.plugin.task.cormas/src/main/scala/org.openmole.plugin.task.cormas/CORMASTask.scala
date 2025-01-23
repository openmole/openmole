package org.openmole.plugin.task.cormas

import monocle.Focus

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.task.container.*
import org.openmole.plugin.task.external.*
import org.json4s._
import org.json4s.jackson.JsonMethods._


object CORMASTask:
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
    containerSystem:        ContainerSystem             = ContainerSystem.default)(using sourcecode.Name, DefinitionScope) =

    ExternalTask.build("CORMASTask"): buildParameters =>
      import buildParameters.*

      val preparedImage =
        import taskExecutionBuildContext.given
        ContainerTask.install(containerSystem, cormasImage("elcep/cormas", version), install = install, clearCache = clearContainerCache)

      def scriptName = "_script_.st"

      def workDirectory = "/_workdirectory_"

      val taskExecution =
        ContainerTask.execution(
          image = preparedImage,
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
          info = info)(taskExecutionBuildContext)

      ExternalTask.execution: p =>
        import p.*
        import Mapped.noFile

        def inputJSONName = "input.json"

        def outputJSONName = "output.json"

        import org.openmole.core.json.*

        def inputsFields: Seq[JField] = noFile(mapped.inputs).map { i ⇒ i.name -> (toJSONValue(context(i.v)): JValue) }

        def inputDictionary = JObject(inputsFields *)

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


        jsonInputs.content = compact(render(inputDictionary))
        scriptFile.content = RunnableScript.content(script)

        def containerTask =
          taskExecution.set(
            resources += (jsonInputs, inputJSONName, true),
            resources += (scriptFile, scriptName, true),
            outputFiles += (outputJSONName, outputFile),
            Mapped.files(mapped.inputs).map(m => inputFiles += (m.v, m.name, true)),
            Mapped.files(mapped.outputs).map(m => outputFiles += (m.name, m.v))
          )

        val resultContext = containerTask(executionContext).from(p.context)(p.random, p.tmpDirectory, p.fileService)
        resultContext ++ readOutputJSON(resultContext(outputFile))
  .set (outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)
  .withValidate: info =>
    ContainerTask.validateContainer(Vector(), environmentVariables, info.external)