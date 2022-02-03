
package org.openmole.plugin.task.python

import monocle.Focus
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

object PythonTask {

  implicit def isTask: InputOutputBuilder[PythonTask] = InputOutputBuilder(Focus[PythonTask](_.config))
  implicit def isExternal: ExternalBuilder[PythonTask] = ExternalBuilder(Focus[PythonTask](_.external))
  implicit def isInfo: InfoBuilder[PythonTask] = InfoBuilder(Focus[PythonTask](_.info))
  implicit def isMapped: MappedInputOutputBuilder[PythonTask] = MappedInputOutputBuilder(Focus[PythonTask](_.mapped))

  def dockerImage(version: String) = DockerImage("python", version)

  def installCommands(install: Seq[String], libraries: Seq[String], major: Int): Vector[String] = {
    // need to install pip2 in case of python 2
    val effintsall = install ++
      (if (major == 2) Seq("curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py","python2 get-pip.py") else Seq.empty)

    (effintsall ++ libraries.map { l ⇒ "pip"+major+" install " + l }).toVector
  }

  def apply(
    script:                 RunnableScript,
    arguments:              OptionalArgument[String] = None,
    version:                String                             = "3.10.2",
    libraries:              Seq[String]                        = Seq.empty,
    install:                Seq[String]                        = Seq.empty,
    workDirectory:          OptionalArgument[String]           = None,
    hostFiles:              Seq[HostFile]                      = Vector.empty,
    environmentVariables:   Seq[EnvironmentVariable] = Vector.empty,
    errorOnReturnValue:     Boolean                            = true,
    returnValue:            OptionalArgument[Val[Int]]         = None,
    stdOut:                 OptionalArgument[Val[String]]      = None,
    stdErr:                 OptionalArgument[Val[String]]      = None,
    containerSystem:        ContainerSystem                  = ContainerSystem.default,
    installContainerSystem: ContainerSystem                  = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService) = {

    val major = if(version.startsWith("2")) 2 else 3

    new PythonTask(
      script = script,
      arguments = arguments.option,
      image = ContainerTask.prepare(installContainerSystem, dockerImage(version), installCommands(install, libraries, major)),
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
      mapped = MappedInputOutputConfig(),
      major = major
    ) set (outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)
  }
}

case class PythonTask(
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
  mapped:                 MappedInputOutputConfig,
  major:                  Int) extends Task with ValidateTask {

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
      noFile(mapped.inputs).zipWithIndex.map {
        case (m, i) ⇒ s"${m.name} = $dicoName[${i}]"
      }.mkString("\n")

    def outputMapping: String =
      s"""[${noFile(mapped.outputs).map { m ⇒ m.name }.mkString(",")}]"""

    val resultContext: Context = p.newFile.withTmpFile("script", ".py") { scriptFile ⇒
      p.newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒

        def inputArrayName = "_generateddata_"
        def scriptName = "_generatescript_.py"
        def inputJSONName = "_inputs_.json"
        def outputJSONName = "_outputs_.json"

        writeInputsJSON(jsonInputs)
        scriptFile.content =
          s"""
             |import json
             |f = open('/$inputJSONName','r')
             |$inputArrayName = json.load(open('/$inputJSONName'))
             |${inputMapping(inputArrayName)}
             |${RunnableScript.content(script)}
             |json.dump($outputMapping, open('/$outputJSONName','w'))
      """.stripMargin

        val outputFile = Val[File]("outputFile", Namespace("PythonTask"))

        val argumentsValue = arguments.map(" " + _).getOrElse("")

        def containerTask =
          ContainerTask(
            containerSystem = containerSystem,
            image = image,
            command = s"python${major.toString} $scriptName" + argumentsValue,
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
              Mapped.files(mapped.inputs).map { case m ⇒ inputFiles.+=[ContainerTask] (m.v, m.name, true) },
              Mapped.files(mapped.outputs).map { case m ⇒ outputFiles.+=[ContainerTask] (m.name, m.v) }
            )


        val resultContext = containerTask.process(executionContext).from(p.context)(p.random, p.newFile, p.fileService)
        resultContext ++ readOutputJSON(resultContext(outputFile))
      }
    }
    resultContext
  }

}


