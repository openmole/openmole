
package org.openmole.plugin.task.julia

import monocle.macros._
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.tools.OptionalArgument
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.plugin.tool.json._
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.plugin.task.container

/**
 * https://docs.julialang.org/en/v1/base/numbers/#Base.isone
 *
 * https://gist.github.com/silgon/0ba43e00e0749cdf4f8d244e67cd9d6a
 */
object JuliaTask {

  implicit def isTask: InputOutputBuilder[JuliaTask] = InputOutputBuilder(JuliaTask.config)
  implicit def isExternal: ExternalBuilder[JuliaTask] = ExternalBuilder(JuliaTask.external)
  implicit def isInfo = InfoBuilder(info)
  implicit def isMapped = MappedInputOutputBuilder(JuliaTask.mapped)

    def installCommands(install: Seq[String], libraries: Seq[String]): Vector[String] = {
       (install ++ Seq("""sh -c "julia -e 'using Pkg; Pkg.add.([ """ + libraries.map { l ⇒ "\\\""+l+"\\\"" }.mkString(",")+"""])'"""" )).toVector
    }

    def apply(
      script:               RunnableScript,
      arguments: OptionalArgument[String] = None,
      libraries:            Seq[String]                        = Seq.empty,
      install:              Seq[String]                        = Seq.empty,
      workDirectory:        OptionalArgument[String]           = None,
      hostFiles:            Seq[HostFile]                      = Vector.empty,
      environmentVariables: Seq[EnvironmentVariable] = Vector.empty,
      errorOnReturnValue:   Boolean                            = true,
      returnValue:          OptionalArgument[Val[Int]]         = None,
      stdOut:               OptionalArgument[Val[String]]      = None,
      stdErr:               OptionalArgument[Val[String]]      = None,
      containerSystem:        ContainerSystem                  = ContainerSystem.default,
      installContainerSystem: ContainerSystem                  = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService) = {

     new JuliaTask(
        script = script,
        arguments = arguments.option,
        image = ContainerTask.prepare(installContainerSystem, DockerImage("julia"), installCommands(install, Seq("JSON")++libraries)),
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
      ) set (outputs += (Seq(returnValue.option, stdOut.option, stdErr.option).flatten: _*))
    }
}

@Lenses case class JuliaTask(
  script:                 RunnableScript,
  image:                  PreparedImage,
  arguments:              Option[String],
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
  mapped:                 MappedInputOutputConfig) extends Task with ValidateTask {

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate = container.validateContainer(Vector(), environmentVariables, external)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import org.json4s.jackson.JsonMethods._
    import p._
    import Mapped.noFile

    def writeInputsJSON(file: File): Unit = {
      def values = noFile(mapped.inputs).map { m => p.context(m.v) }
      file.content = compact(render(toJSONValue(values.toArray)))
    }

    def readOutputJSON(file: File) = {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      val outputValues = parse(file.content)
      (outputValues.asInstanceOf[JArray].arr zip noFile(mapped.outputs).map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v) }
    }

    def inputMapping(dicoName: String): String =
      noFile(mapped.inputs).map {
        case m ⇒ s"${m.name} = $dicoName[\"${m.name}\"]"
      }.mkString("\n")

    def outputMapping: String =
      s"""Dict(${noFile(mapped.outputs).map { m ⇒ "\""+m.name+"\" => "+m.v }.mkString(",")})"""

    val resultContext: Context = p.newFile.withTmpFile("script", ".jl") { scriptFile ⇒
      p.newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒

        def inputArrayName = "_generateddata_"
        def scriptName = "_generatescript_.jl"
        def inputJSONName = "_inputs_.json"
        def outputJSONName = "_outputs_.json"

        writeInputsJSON(jsonInputs)
        scriptFile.content =
          s"""
             |import JSON
             |$inputArrayName = "/$inputJSONName" |> open |> JSON.parse
             |${inputMapping(inputArrayName)}
             |${RunnableScript.content(script)}
             |write(open("/$outputJSONName","w"),JSON.json($outputMapping))
      """.stripMargin

        val outputFile = Val[File]("outputFile", Namespace("JuliaTask"))

        val argumentsValue = arguments.map(" " + _).getOrElse("")

        def containerTask =
          ContainerTask(
            containerSystem = containerSystem,
            image = image,
            command = s"sh -c \"julia $scriptName" + argumentsValue+"\"",
            workDirectory = None,
            relativePathRoot = None,
            errorOnReturnValue = errorOnReturnValue,
            returnValue = returnValue,
            hostFiles = hostFiles,
            environmentVariables = environmentVariables,
            reuseContainer = true,
            stdOut = stdOut,
            stdErr = stdErr,
            config = InputOutputConfig(),
            external = external,
            info = info,
            containerPoolKey = containerPoolKey) set (
              resources += (scriptFile, scriptName, true),
              resources += (jsonInputs, inputJSONName, true),
              outputFiles += (outputJSONName, outputFile),
              Mapped.files(mapped.inputs).map { case m ⇒ inputFiles +=[ContainerTask] (m.v, m.name, true) },
              Mapped.files(mapped.outputs).map { case m ⇒ outputFiles +=[ContainerTask] (m.name, m.v) }
            )


        val resultContext = containerTask.process(executionContext).from(p.context)(p.random, p.newFile, p.fileService)
        resultContext ++ readOutputJSON(resultContext(outputFile))
      }
    }
    resultContext
  }

}


