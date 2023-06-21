package org.openmole.plugin.task.r

import monocle.Focus
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
import org.openmole.plugin.task.container.ContainerTask.install
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.core.json.*
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.core.dsl.extension._

import org.json4s.jackson.JsonMethods._
import org.json4s._
import org.openmole.core.workflow.tools.OptionalArgument

object RTask:

  given InputOutputBuilder[RTask] = InputOutputBuilder(Focus[RTask](_.config))
  given ExternalBuilder[RTask] = ExternalBuilder(Focus[RTask](_.external))
  given InfoBuilder[RTask] = InfoBuilder(Focus[RTask](_.info))
  given MappedInputOutputBuilder[RTask] = MappedInputOutputBuilder(Focus[RTask](_.mapped))

  case class RLibrary(name: String, version: Option[String] = None, dependencies: Boolean = false, system: Boolean = false)

  object RLibrary:

    def toCommand(installCommand: RLibrary) =
      def dependencies(d: Boolean) = if(d) "T" else "NA"

      installCommand match
        case RLibrary(name, None, d, _) ⇒
          s"""R --slave -e 'install.packages(c("$name"), dependencies = ${dependencies(d)}); library("$name")'"""
        case RLibrary(name, Some(version), d, _) ⇒
          s"""R --slave -e 'library(remotes); remotes::install_version("$name",version = "$version", dependencies = ${dependencies(d)}); library("$name")'"""

    implicit def stringToRLibrary(name: String): RLibrary = RLibrary(name, None)
    implicit def stringCoupleToRLibrary(couple: (String, String)): RLibrary = RLibrary(couple._1, Some(couple._2))
    implicit def stringOptionCoupleToRLibrary(couple: (String, Option[String])): RLibrary = RLibrary(couple._1, couple._2)
    implicit def tupleToRLibraryVersionDep(tuple: (String, String, Boolean)): RLibrary = RLibrary(tuple._1, Some(tuple._2), tuple._3)
    implicit def tupleToRLibraryVersionDepSystem(tuple: (String, String, Boolean, Boolean)): RLibrary = RLibrary(tuple._1, Some(tuple._2), tuple._3, tuple._4)
    implicit def tupleToRLibraryDep(tuple: (String, Boolean)): RLibrary = RLibrary(tuple._1, None, tuple._2)
    implicit def tupleToRLibraryDepSystem(tuple: (String, Boolean, Boolean)): RLibrary = RLibrary(tuple._1, None, tuple._2, tuple._3)


    // not needed, for each package separate call to get sys
    //case class Sysdep(rpackage: String) extends InstallCommand

    def installCommands(libraries: Vector[RLibrary]): Vector[String] = libraries.map(RLibrary.toCommand)


  def rImage(image: String, version: String) = DockerImage(image, version)

  def apply(
    script:                     RunnableScript,
    install:                    Seq[String]                        = Seq.empty,
    libraries:                  Seq[RLibrary]                      = Seq.empty,
    prepare:                    Seq[String]                        = Seq.empty,
    installSystemDependencies:  Boolean                            = false,
    image:                      String                             = "openmole/r-base",
    version:                    String                             = "4.2.2",
    errorOnReturnValue:         Boolean                            = true,
    returnValue:                OptionalArgument[Val[Int]]         = None,
    stdOut:                     OptionalArgument[Val[String]]      = None,
    stdErr:                     OptionalArgument[Val[String]]      = None,
    hostFiles:                  Seq[HostFile]                      = Vector.empty,
    environmentVariables:       Seq[EnvironmentVariable]           = Vector.empty,
    clearContainerCache:        Boolean                            = false,
    containerSystem:            ContainerSystem                    = ContainerSystem.default,
    installContainerSystem:     ContainerSystem                    = ContainerSystem.default,
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService): RTask =

    val systemDependenciesForLibraries =
      if installSystemDependencies
      then libraries
      else libraries.filter(_.system)

    val sysdeps: String =
      if systemDependenciesForLibraries.nonEmpty
      then
        // Get system dependencies using the rstudio packagemanager API, inspired from https://github.com/mdneuzerling/getsysreqs
        // API doc: https://packagemanager.rstudio.com/__api__/swagger/index.html
        val apicallurl = "http://packagemanager.rstudio.com/__api__/repos/1/sysreqs?all=false&" +
          systemDependenciesForLibraries.map { l => "pkgname=" + l.name + "&" }.mkString("") +
          "distribution=ubuntu&release=20.04"
        try
          val jsonDeps = NetworkService.get(apicallurl)
          val reqs = parse(jsonDeps).asInstanceOf[JObject].values
          if (reqs.contains("requirements")) {
            reqs("requirements").asInstanceOf[List[_]].map {
              _.asInstanceOf[Map[String, Any]]("requirements").asInstanceOf[Map[String, Any]]("packages").asInstanceOf[List[String]].mkString(" ")
            }.mkString(" ")
          }
          else throw InternalProcessingError(s"Error while fetching system dependencies for R packages $libraries\nInconsistent API response\nTry setting the autoInstallSystemDeps argument to false and adjusting customised install argument accordingly.")
        catch
          case t: Throwable =>
            throw InternalProcessingError(s"Error while fetching system dependencies for R packages $libraries\nTry setting the autoInstallSystemDeps argument to false and adjusting customised install argument accordingly.", t)
      else ""

    val installCommands = install ++
      (if (sysdeps.nonEmpty) Seq("apt update", "apt-get -y install "+sysdeps).map(c => ContainerSystem.sudo(containerSystem, c)) else Seq.empty) ++
      RLibrary.installCommands(libraries.toVector)

    RTask(
      script = script,
      image = ContainerTask.install(installContainerSystem, rImage(image, version), installCommands, clearCache = clearContainerCache),
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


case class RTask(
  script:               RunnableScript,
  image:                InstalledImage,
  errorOnReturnValue:   Boolean,
  prepare:              Seq[String],
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  hostFiles:            Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  containerSystem:      ContainerSystem,
  config:               InputOutputConfig,
  external:             External,
  info:                 InfoConfig,
  mapped:               MappedInputOutputConfig) extends Task with ValidateTask:

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate = container.validateContainer(Vector(), environmentVariables, external)

  override def process(executionContext: TaskExecutionContext) = FromContext: p ⇒
    import Mapped.noFile
    import org.json4s.jackson.JsonMethods._
    import p._

    def writeInputsJSON(file: File) =
      def values = noFile(mapped.inputs).map { m ⇒ m.v.`type`.manifest.array(context(m.v)) }
      file.content = compact(render(toJSONValue(values.toArray[Any])))

    def rInputMapping(arrayName: String) =
      noFile(mapped.inputs).zipWithIndex.map { case (m, i) ⇒ s"${m.name} = $arrayName[[${i + 1}]][[1]]" }.mkString("\n")

    def rOutputMapping =
      s"""list(${noFile(mapped.outputs).map { _.name }.mkString(",")})"""

    def readOutputJSON(file: File) =
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      val outputValues = parse(file.content)
      (outputValues.asInstanceOf[JArray].arr zip noFile(mapped.outputs).map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v, unwrapArrays = true) }

    val jsonInputs = executionContext.taskExecutionDirectory.newFile("input", ".json")
    val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".R")

    def workDirectory = "/_workdirectory_"
    def inputArrayName = "generatedomarray"
    def rScriptPath = s"$workDirectory/_generatedomscript_.R"
    def inputJSONPath = s"$workDirectory/_generatedominputs_.json"
    def outputJSONPath = s"$workDirectory/_generatedomoutputs_.json"

    writeInputsJSON(jsonInputs)

    scriptFile.content = s"""
      |library("jsonlite")
      |$inputArrayName = fromJSON("$inputJSONPath", simplifyMatrix = FALSE)
      |${rInputMapping(inputArrayName)}
      |${RunnableScript.content(script)}
      |write_json($rOutputMapping, "$outputJSONPath", always_decimal = TRUE)
      """.stripMargin

    val outputFile = Val[File]("outputFile", Namespace("RTask"))

    def containerTask =
      ContainerTask.isolatedWorkdirectory(executionContext)(
        containerSystem = containerSystem,
        image = image,
        command = prepare ++ Seq(s"R --slave -f $rScriptPath"),
        workDirectory = workDirectory,
        errorOnReturnValue = errorOnReturnValue,
        returnValue = returnValue,
        hostFiles = hostFiles,
        environmentVariables = environmentVariables,
        stdOut = stdOut,
        stdErr = stdErr,
        config = config,
        external = external,
        info = info,
        containerPoolKey = containerPoolKey) set (
        resources += (scriptFile, rScriptPath),
        resources += (jsonInputs, inputJSONPath),
        outputFiles += (outputJSONPath, outputFile),
        Mapped.files(mapped.inputs).map { m ⇒ inputFiles += (m.v, m.name, true) },
        Mapped.files(mapped.outputs).map { m ⇒ outputFiles += (m.name, m.v) }
      )

    val resultContext =
      try containerTask.process(executionContext).from(context)
      catch
        case t: UserBadDataError => throw UserBadDataError(s"Error executing script:\n${scriptFile.content}", t)
        case t: Throwable => throw InternalProcessingError(s"Error executing script:\n${scriptFile.content}", t)

    resultContext ++ readOutputJSON(resultContext(outputFile))

