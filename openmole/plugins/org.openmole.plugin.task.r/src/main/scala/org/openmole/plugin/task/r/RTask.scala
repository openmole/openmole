package org.openmole.plugin.task.r

import monocle.macros.Lenses
import org.openmole.core.context.{Namespace, Val}
import org.openmole.core.dsl._
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.expansion._
import org.openmole.core.fileservice._
import org.openmole.core.networkservice._
import org.openmole.core.preference._
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace._
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container.ContainerTask.prepare
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.plugin.tool.json._
import org.openmole.tool.outputredirection.OutputRedirection

import org.json4s.jackson.JsonMethods._
import org.json4s._

object RTask {

  implicit def isTask: InputOutputBuilder[RTask] = InputOutputBuilder(RTask.config)
  implicit def isExternal: ExternalBuilder[RTask] = ExternalBuilder(RTask.external)
  implicit def isInfo = InfoBuilder(info)
  implicit def isMapped = MappedInputOutputBuilder(RTask.mapped)

  sealed trait InstallCommand
  object InstallCommand {
    case class RLibrary(name: String, version: Option[String] = None, dependencies: Boolean = false) extends InstallCommand

    def toCommand(installCommand: InstallCommand) = {
      def dependencies(d: Boolean) = if(d) "T" else "NA"

      installCommand match {
        case RLibrary(name, None, d) ⇒
          s"""R --slave -e 'install.packages(c("$name"), dependencies = ${dependencies(d)}); library("$name")'"""
        case RLibrary(name, Some(version), d) ⇒
          s"""R --slave -e 'library(remotes); remotes::install_version("$name",version = "$version", dependencies = ${dependencies(d)}); library("$name")'"""
        //case Sysdep(name) => ""
      }
    }

    implicit def stringToRLibrary(name: String): InstallCommand = RLibrary(name, None)
    implicit def stringCoupleToRLibrary(couple: (String, String)): InstallCommand = RLibrary(couple._1, Some(couple._2))
    implicit def stringOptionCoupleToRLibrary(couple: (String, Option[String])): InstallCommand = RLibrary(couple._1, couple._2)
    implicit def tupleToRLibrary(tuple: (String, String, Boolean)): InstallCommand = RLibrary(tuple._1, Some(tuple._2), tuple._3)

    // not needed, for each package separate call to get sys
    //case class Sysdep(rpackage: String) extends InstallCommand

    def installCommands(libraries: Vector[InstallCommand]): Vector[String] = libraries.map(InstallCommand.toCommand)
  }

  def rImage(image: String, version: String) = DockerImage(image, version)

  def apply(
    script:               RunnableScript,
    install:              Seq[String]                        = Seq.empty,
    libraries:            Seq[InstallCommand]                = Seq.empty,
    image:                String                             = "openmole/r-base",
    version:              String                             = "4.1.2",
    errorOnReturnValue:   Boolean                            = true,
    returnValue:          OptionalArgument[Val[Int]]         = None,
    stdOut:               OptionalArgument[Val[String]]      = None,
    stdErr:               OptionalArgument[Val[String]]      = None,
    hostFiles:            Seq[HostFile]                      = Vector.empty,
    workDirectory:        OptionalArgument[String]           = None,
    environmentVariables: Seq[EnvironmentVariable]           = Vector.empty,
    clearContainerCache:    Boolean                          = false,
    containerSystem:        ContainerSystem                  = ContainerSystem.default,
    installContainerSystem: ContainerSystem                  = ContainerSystem.default,
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService): RTask = {

    // get system dependencies using the rstudio packagemanager API
    // API doc: https://packagemanager.rstudio.com/__api__/swagger/index.html
    // inspired from https://github.com/mdneuzerling/getsysreqs 
    // ! networkService is not used here to get the http url: pb with proxy?
    //println(gridscale.http.HTTP())
    val sysdeps: String = parse(gridscale.http.get[String]("http://packagemanager.rstudio.com/__api__/repos/1/sysreqs?all=false&"+
      libraries.map{case InstallCommand.RLibrary(name,_,_) => "pkgname="+name+"&"}.mkString("")+"distribution=ubuntu&release=20.04")).
      asInstanceOf[JObject].values.get("requirements").asInstanceOf[JArray].arr.map(_.asInstanceOf[JObject].values.get("requirements").asInstanceOf[JObject].values.get("packages").asInstanceOf[JArray].arr.map(_.toString).mkString(" ")).mkString(" ")

    println("SYSDEPS = "+sysdeps)

    val installCommands = install ++ Seq("apt update", "apt-get -y install "+sysdeps.mkString(" ")).map(c => ContainerSystem.sudo(containerSystem, c)) ++
      InstallCommand.installCommands(libraries.toVector)

    RTask(
      script = script,
      image = ContainerTask.prepare(installContainerSystem, rImage(image, version), installCommands, clearCache = clearContainerCache),
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

  override def validate = container.validateContainer(Vector(), environmentVariables, external)

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
            relativePathRoot = None,
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

        val resultContext =
          try containerTask.process(executionContext).from(context)
          catch {
            case t: UserBadDataError => throw UserBadDataError(s"Error executing script:\n${scriptFile.content}", t)
            case t: Throwable => throw InternalProcessingError(s"Error executing script:\n${scriptFile.content}", t)
          }
        resultContext ++ readOutputJSON(resultContext(outputFile))
      }
    }
  }
}