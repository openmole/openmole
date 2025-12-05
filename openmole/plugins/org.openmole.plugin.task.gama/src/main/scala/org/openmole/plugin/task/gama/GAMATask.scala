package org.openmole.plugin.task.gama

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.setter.*
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container.*
import org.openmole.plugin.task.external.*

import scala.xml.XML

object GAMATask:
  def workspaceDirectory = "/_workspace_"
  def inputXML = s"/_model_input_.xml"
  def gamaWorkspaceName = "_gama_workspace_"
  def gamaWorkspaceDirectory = s"$workspaceDirectory/$gamaWorkspaceName"

  def volumes(
    workspace: File,
    model:     String) =
    val content = workspace.listFiles.map { f => f -> s"$gamaWorkspaceDirectory/${f.getName}"}.toSeq
    (model, content)

  def prepare(
    workspace:              File,
    model:                  String,
    install:                Seq[String],
    containerSystem:        Option[ContainerSystem],
    image:                  ContainerImage,
    clearCache:             Boolean)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, _workspace: Workspace, fileService: FileService) =

    def fixIni = Seq("""sed -i -E '/-XX:\+UseG1GC/ d; /-XX:G1[^ ]*/ d' /opt/gama-platform/Gama.ini""")

    val installedImage =
      ContainerTask.install(
        containerSystem,
        image,
        fixIni ++ install,
        Seq(),
        clearCache = clearCache)

    installedImage

  def toGAMA(v: Any): String =
    v match
      case v: Int => v.toString
      case v: Long => v.toString
      case v: Double => v.toString
      case v: Boolean => if v then "true" else "false"
      case v: String => '"' + v + '"'
      case v: Array[?] => "[" + v.map(toGAMA).mkString(", ") + "]"
      case _ => throw new UserBadDataError(s"Value $v of type ${v.getClass} is not convertible to Scilab")

  def apply(
    project:                File,
    gaml:                   String,
    finalStep:              OptionalArgument[FromContext[Int]]    = None,
    stop:                   OptionalArgument[FromContext[String]] = None,
    seed:                   OptionalArgument[Val[Long]]           = None,
    install:                Seq[String]                           = Seq.empty,
    containerImage:         ContainerImage                        = "gamaplatform/gama:2025.06.4",
    memory:                 OptionalArgument[Information]         = 1.gigabyte,
    version:                OptionalArgument[String]              = None,
    errorOnReturnValue:     Boolean                               = true,
    returnValue:            OptionalArgument[Val[Int]]            = None,
    stdOut:                 OptionalArgument[Val[String]]         = None,
    stdErr:                 OptionalArgument[Val[String]]         = None,
    environmentVariables:   Seq[EnvironmentVariable]              = Vector.empty,
    hostFiles:              Seq[HostFile]                         = Vector.empty,
    fewerThreads:           Boolean                               = true,
    //    workDirectory:          OptionalArgument[String]        = None,
    clearContainerCache:    Boolean                               = false,
    containerSystem:        OptionalArgument[ContainerSystem]     = None)(using sourcecode.Name, DefinitionScope) =

    ExternalTask.build("GAMATask"): buildParameters =>
      import buildParameters.*

      if !project.exists() then throw new UserBadDataError(s"The project directory you specify does not exist: ${project}")
      if !(project / gaml).exists() then throw new UserBadDataError(s"The model file you specify does not exist: ${project / gaml}")

      val gamaContainerImage: ContainerImage =
        (version.option, containerImage) match
          case (None, c) => c
          case (Some(v), c: DockerImage) => c.copy(tag = v)
          case (Some(_), _: SavedDockerImage) => throw new UserBadDataError(s"Can not set both, a saved docker image, and, set the version of the container.")

      val preparedImage =
        import taskExecutionBuildContext.given
        prepare(project, gaml, install, containerSystem, gamaContainerImage, clearCache = clearContainerCache)

      val inputFilePath = s"${GAMALegacyTask.gamaWorkspaceDirectory}/__om_experiment__.gaml"

      def outputDirectoryPath = s"${GAMALegacyTask.workspaceDirectory}/_output_"

      def memoryValue =
        memory.option match
          case None => ""
          case Some(m) => s"-m ${m.toMegabytes.toLong}m"

      def omExperimentName = "_openMOLEExperiment_"

      def launchCommand =
        s"gama-headless $memoryValue -hpc 1 -batch $omExperimentName $inputFilePath"

      def environmentVariablesValue =
        def fewerThreadsEnvironmentVariable: EnvironmentVariable =
          val exquinoxSingleThread = Seq("-Dequinox.resolver.thread.count=1", "-Dequinox.start.level.thread.count=1", "-Dequinox.start.level.restrict.parallel=true")
          ("_JAVA_OPTIONS", (JavaConfiguration.fewerThreadsParameters ++ exquinoxSingleThread).mkString(" "))
        environmentVariables ++
          (if fewerThreads then Seq(fewerThreadsEnvironmentVariable) else Seq())

      val containerTaskExecution =
        ContainerTask.execution(
          image = preparedImage,
          command = launchCommand,
          workDirectory = Some(GAMALegacyTask.workspaceDirectory),
          relativePathRoot = Some(GAMALegacyTask.gamaWorkspaceDirectory),
          errorOnReturnValue = errorOnReturnValue,
          returnValue = returnValue,
          hostFiles = hostFiles,
          environmentVariables = environmentVariablesValue,
          stdOut = stdOut,
          stdErr = stdErr,
          config = config,
          external = external,
          info = info)

      ExternalTask.execution: executionParameters =>
        import executionParameters.*

        val inputFile = executionContext.taskExecutionDirectory.newFile("input", ".gaml")
        val outputDirectory = executionContext.taskExecutionDirectory.newDirectory("output", create = true)

        val seedValue = math.abs(seed.map(_.from(context)).getOrElse(random().nextLong))

        def inputParameters =
          for m <- Mapped.noFile(mapped.inputs)
          yield s"""parameter var:${m.name} <- ${GAMATask.toGAMA(context(m.v))};"""

        def outputMapping =
          for m <- Mapped.noFile(mapped.outputs)
          yield s"\"${m.name}\"::${m.name}"

        def outputFileName = "__om_output__.json"
        def outputFilePath = s"$outputDirectoryPath/$outputFileName"

        def readOutputJSON(file: File) =
          import org.json4s.*
          import org.json4s.jackson.JsonMethods.*
          import org.openmole.core.json.*
          def outputValues = parse(file.content)
          val outputMap: Map[String, JValue] = outputValues.asInstanceOf[JObject].obj.toMap
          Mapped.noFile(mapped.outputs).map { m => jValueToVariable(outputMap(m.name), m.v, unwrapArrays = true) }


        val (_, volumes) = GAMALegacyTask.volumes(project, gaml)

        def stopCondition =
          (finalStep.option, stop.option) match
            case (_, Some(s)) => s"when:${s.from(context)}"
            case (Some(s), None) => s"when:cycle=${s.from(context)}"
            case (None, None) => throw InternalProcessingError("No stopping condition is defined")

        def experimentFileContent  =
          s"""
            |model openmoleexplorationmodel
            |
            |import '$gaml'
            |
            |experiment $omExperimentName {
            |
            |  float seed <- $seedValue;
            |${inputParameters.map("  " + _).mkString("\n")}
            |
            |	 reflex stop_reflex $stopCondition {
            |
            |    map<string, unknown> _outputs_ <- [
            |${outputMapping.map("    " + _).mkString(",\n")}
            |	 	 ];
            |
            |		 save to_json(_outputs_) to:"${outputFilePath}" format:"txt";
            |
            |		 do die;
            |	 }
            |}
            |""".stripMargin

        inputFile.content = experimentFileContent

        def containerTask =
          containerTaskExecution.set(
            resources += (inputFile, inputFilePath, true),
            resources += (outputDirectory, outputDirectoryPath, true),
            volumes.map((lv, cv) => resources += (lv, cv, true)),
            Mapped.files(mapped.inputs).map { m => inputFiles += (m.v, m.name, true) },
            Mapped.files(mapped.outputs).map { m => outputFiles += (m.name, m.v) }
          )

        try
          val resultContext = containerTask(executionContext).from(context)
          resultContext ++ readOutputJSON(outputDirectory / outputFileName)
        catch
          case t: Throwable =>
            throw InternalProcessingError(
              s"""Error executing GAMA headless, you should look at the standard output.
                   |Experiment file content was: $experimentFileContent
                   |GAMA was launched using the command: $launchCommand""", t)
    .withValidate: info =>
      def stopError: Seq[Throwable] =
        if !finalStep.isDefined && !stop.isDefined then Seq(UserBadDataError("You should set either the finalStep or stop parameter")) else Seq()

      ContainerTask.validateContainer(Vector(), environmentVariables, info.external) ++
        finalStep.map(_.validate).toSeq ++
        stop.map(_.validate).toSeq ++
        stopError
    .set(
      inputs ++= seed.option.toSeq,
      outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten
    )





