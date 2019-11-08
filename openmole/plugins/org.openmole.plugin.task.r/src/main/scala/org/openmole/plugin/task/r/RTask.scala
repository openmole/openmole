package org.openmole.plugin.task.r

import monocle.macros.Lenses
import org.openmole.core.context.{Namespace, Val}
import org.openmole.core.dsl._
import org.openmole.core.expansion._
import org.openmole.core.fileservice._
import org.openmole.core.networkservice._
import org.openmole.core.preference._
import org.openmole.core.threadprovider._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace._
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container.ContainerTask.prepare
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.udocker._
import org.openmole.plugin.tool.json._
import org.openmole.tool.outputredirection.OutputRedirection

object RTask {

  implicit def isTask: InputOutputBuilder[RTask] = InputOutputBuilder(RTask.config)
  implicit def isExternal: ExternalBuilder[RTask] = ExternalBuilder(RTask.external)
  implicit def isInfo = InfoBuilder(info)
  implicit def isMapped = MappedInputOutputBuilder(RTask.mapped)

  sealed trait InstallCommand
  object InstallCommand {
    case class RLibrary(name: String, version: Option[String]) extends InstallCommand

    def toCommand(installCommands: InstallCommand) =
      installCommands match {
        case RLibrary(name, None) ⇒
          //Vector(s"""R -e 'install.packages(c(${names.map(lib ⇒ '"' + s"$lib" + '"').mkString(",")}), dependencies = T)'""")
          s"""R --slave -e 'install.packages(c("$name"), dependencies = T); library("$name")'"""
        case RLibrary(name, Some(version)) ⇒
          // need to install devtools to get older packages versions
          //apt update; apt-get -y install libssl-dev libxml2-dev libcurl4-openssl-dev libssh2-1-dev;
          s"""R --slave -e 'library(devtools); install_version("$name",version = "$version", dependencies = T); library("$name")'"""
      }

    implicit def stringToRLibrary(name: String): InstallCommand = RLibrary(name, None)
    implicit def stringCoupleToRLibrary(couple: (String, String)): InstallCommand = RLibrary(couple._1, Some(couple._2))
    implicit def stringOptionCoupleToRLibrary(couple: (String, Option[String])): InstallCommand = RLibrary(couple._1, couple._2)

    def installCommands(libraries: Vector[InstallCommand]): Vector[String] = libraries.map(InstallCommand.toCommand)
  }

  def rImage(version: String) = DockerImage("openmole/r-base", version)

  def apply(
    script:               RunnableScript,
    install:              Seq[String]                        = Seq.empty,
    libraries:            Seq[InstallCommand]                = Seq.empty,
    forceUpdate:          Boolean                            = false,
    version:              String                             = "3.5.1",
    errorOnReturnValue:   Boolean                            = true,
    returnValue:          OptionalArgument[Val[Int]]         = None,
    stdOut:               OptionalArgument[Val[String]]      = None,
    stdErr:               OptionalArgument[Val[String]]      = None,
    hostFiles:            Seq[HostFile]                      = Vector.empty,
    workDirectory:        OptionalArgument[String]           = None,
    environmentVariables: Seq[EnvironmentVariable] = Vector.empty,
    containerSystem:    ContainerSystem                                    = Proot()
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService): RTask = {

    // add additional installation of devtools only if needed
    val installCommands =
      if (libraries.exists { case l: InstallCommand.RLibrary ⇒ l.version.isDefined }) {
        install ++ Seq("apt update", "apt-get -y install libssl-dev libxml2-dev libcurl4-openssl-dev libssh2-1-dev",
          """R --slave -e 'install.packages("devtools", dependencies = T); library(devtools);""") ++
          InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite", None)))
      }
      else install ++ InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite", None)))

    RTask(
      script = script,
      ContainerTask.prepare(containerSystem, rImage(version), installCommands, workDirectory.option),
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

@Lenses case class RTask(
  script:               RunnableScript,
  image:                PreparedImage,
  errorOnReturnValue:   Boolean,
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  hostFiles:            Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  containerSystem:      ContainerSystem,
  config:               InputOutputConfig,
  external:             External,
  info:                 InfoConfig,
  mapped:               MappedInputOutputConfig) extends Task with ValidateTask {

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate = container.validateContainer(Vector(), environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import Mapped.noFile
    import org.json4s.jackson.JsonMethods._
    import p._

    def writeInputsJSON(file: File) = {
      def values = noFile(mapped.inputs).map { m ⇒ Array(context(m.v)) }
      file.content = compact(render(toJSONValue(values.toArray)))
    }

    def rInputMapping(arrayName: String) =
      noFile(mapped.inputs).zipWithIndex.map { case (m, i) ⇒ s"${m.name} = $arrayName[[${i + 1}]][[1]]" }.mkString("\n")

    def rOutputMapping =
      s"""list(${noFile(mapped.outputs).map { _.name }.mkString(",")})"""

    def readOutputJSON(file: File) = {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      val outputValues = parse(file.content)
      (outputValues.asInstanceOf[JArray].arr zip noFile(mapped.outputs).map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v) }
    }

    newFile.withTmpFile("script", ".R") { scriptFile ⇒
      newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒

        def inputArrayName = "generatedomarray"
        def rScriptName = "_generatedomscript_.R"
        def inputJSONName = "_generatedominputs_.json"
        def outputJSONName = "_generatedomoutputs_.json"

        writeInputsJSON(jsonInputs)
        scriptFile.content = s"""
          |library("jsonlite")
          |$inputArrayName = fromJSON("/$inputJSONName", simplifyMatrix = FALSE)
          |${rInputMapping(inputArrayName)}
          |${RunnableScript.content(script)}
          |write_json($rOutputMapping, "/$outputJSONName", always_decimal = TRUE)
          """.stripMargin

        val outputFile = Val[File]("outputFile", Namespace("RTask"))

        def containerTask =
          ContainerTask(
            containerSystem = containerSystem,
            image = image,
            command = s"R --slave -f $rScriptName",
            workDirectory = None,
            errorOnReturnValue = errorOnReturnValue,
            returnValue = returnValue,
            hostFiles = hostFiles,
            environmentVariables = environmentVariables,
            reuseContainer = true,
            stdOut = stdOut,
            stdErr = stdErr,
            config = config,
            external = external,
            info = info,
            containerPoolKey = containerPoolKey) set (
            resources += (scriptFile, rScriptName, true),
            resources += (jsonInputs, inputJSONName, true),
            outputFiles += (outputJSONName, outputFile),
            Mapped.files(mapped.inputs).map { case m ⇒ inputFiles +=[ContainerTask] (m.v, m.name, true) },
            Mapped.files(mapped.outputs).map { case m ⇒ outputFiles +=[ContainerTask] (m.name, m.v) }
          )

        val resultContext = containerTask.process(executionContext).from(context)
        resultContext ++ readOutputJSON(resultContext(outputFile))
      }
    }
  }
}