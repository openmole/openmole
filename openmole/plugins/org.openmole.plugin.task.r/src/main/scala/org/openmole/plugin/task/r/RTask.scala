package org.openmole.plugin.task.r

import monocle.macros.Lenses
import org.openmole.core.context.{ Namespace, Variable }
import org.openmole.plugin.task.udocker._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.core.networkservice._
import org.openmole.plugin.task.external._
import org.openmole.core.expansion._
import org.openmole.core.threadprovider._
import org.openmole.tool.hash._
import org.openmole.core.dsl._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.outputredirection._
import org.openmole.core.workflow.builder._
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.systemexec._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.container
import org.openmole.plugin.tool.json._

object RTask {

  implicit def isTask: InputOutputBuilder[RTask] = InputOutputBuilder(RTask._config)
  implicit def isExternal: ExternalBuilder[RTask] = ExternalBuilder(RTask.external)
  implicit def isInfo = InfoBuilder(info)
  implicit def isMapped = MappedInputOutputBuilder(RTask.mapped)

  implicit def isBuilder = new ReturnValue[RTask] with ErrorOnReturnValue[RTask] with StdOutErr[RTask] with EnvironmentVariables[RTask] with HostFiles[RTask] with WorkDirectory[RTask] { builder ⇒
    override def returnValue = RTask.returnValue
    override def errorOnReturnValue = RTask.errorOnReturnValue
    override def stdOut = RTask.stdOut
    override def stdErr = RTask.stdErr
    override def environmentVariables = RTask.uDocker composeLens UDockerArguments.environmentVariables
    override def hostFiles = RTask.uDocker composeLens UDockerArguments.hostFiles
    override def workDirectory = RTask.uDocker composeLens UDockerArguments.workDirectory
  }

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
    script:               FromContext[String],
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
    environmentVariables: Seq[(String, FromContext[String])] = Vector.empty
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService): RTask = {

    // add additional installation of devtools only if needed
    val installCommands =
      if (libraries.exists { case l: InstallCommand.RLibrary ⇒ l.version.isDefined }) {
        install ++ Seq("apt update", "apt-get -y install libssl-dev libxml2-dev libcurl4-openssl-dev libssh2-1-dev",
          """R --slave -e 'install.packages("devtools", dependencies = T); library(devtools);""") ++
          InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite", None)))
      }
      else install ++ InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite", None)))

    val uDockerArguments =
      UDockerTask.createUDocker(
        rImage(version),
        install = installCommands,
        cacheInstall = true,
        forceUpdate = forceUpdate,
        mode = "P1",
        reuseContainer = true
      ).copy(
          environmentVariables = environmentVariables.toVector,
          hostFiles = hostFiles.toVector,
          workDirectory = workDirectory)

    RTask(
      script = script,
      uDockerArguments,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      stdOut = stdOut,
      stdErr = stdErr,
      _config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      mapped = MappedInputOutputConfig()
    )
  }

}

@Lenses case class RTask(
  script:             FromContext[String],
  uDocker:            UDockerArguments,
  errorOnReturnValue: Boolean,
  returnValue:        Option[Val[Int]],
  stdOut:             Option[Val[String]],
  stdErr:             Option[Val[String]],
  _config:            InputOutputConfig,
  external:           External,
  info:               InfoConfig,
  mapped:             MappedInputOutputConfig) extends Task with ValidateTask {

  lazy val containerPoolKey = UDockerTask.newCacheKey

  override def config = UDockerTask.config(_config, returnValue, stdOut, stdErr)
  override def validate = container.validateContainer(Vector(), uDocker.environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    def writeInputsJSON(file: File) = {
      def values = mapped.inputs.map { m ⇒ Array(context(m.v)) }
      file.content = compact(render(toJSONValue(values.toArray)))
    }

    def rInputMapping(arrayName: String) =
      mapped.inputs.zipWithIndex.map { case (m, i) ⇒ s"${m.name} = $arrayName[[${i + 1}]][[1]]" }.mkString("\n")

    def rOutputMapping =
      s"""list(${mapped.outputs.map { _.name }.mkString(",")})"""

    def readOutputJSON(file: File) = {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      val outputValues = parse(file.content)
      (outputValues.asInstanceOf[JArray].arr zip mapped.outputs.map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v) }
    }

    newFile.withTmpFile("script", ".R") { scriptFile ⇒
      newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒

        def inputArrayName = "generatedomarray"
        def rScriptName = "generatedomscript.R"
        def inputJSONName = "generatedominputs.json"
        def outputJSONName = "generatedomoutputs.json"

        writeInputsJSON(jsonInputs)
        scriptFile.content = s"""
          |library("jsonlite")
          |$inputArrayName = fromJSON("/$inputJSONName", simplifyMatrix = FALSE)
          |${rInputMapping(inputArrayName)}
          |${script.from(p.context)(p.random, p.newFile, p.fileService)}
          |write_json($rOutputMapping, "/$outputJSONName", always_decimal = TRUE)
          """.stripMargin

        val outputFile = Val[File]("outputFile", Namespace("RTask"))

        def uDockerTask =
          UDockerTask(
            uDocker, s"R --slave -f $rScriptName",
            errorOnReturnValue,
            returnValue,
            stdOut,
            stdErr,
            _config,
            external,
            info,
            containerPoolKey = containerPoolKey) set (
            resources += (scriptFile, rScriptName, true),
            resources += (jsonInputs, inputJSONName, true),
            outputFiles += (outputJSONName, outputFile)
          )

        val resultContext = uDockerTask.process(executionContext).from(context)
        resultContext ++ readOutputJSON(resultContext(outputFile))
      }
    }
  }
}