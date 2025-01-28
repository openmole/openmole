
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

  object Library:
    given Conversion[String, Library] = s => LibraryName(s)
    given Conversion[File, Library] = f => FileLibrary(f)

    case class LibraryName(v: String) extends Library
    case class FileLibrary(f: File) extends Library
    case class PackageSpec(url: String, rev: OptionalArgument[String] = None) extends Library

    def volumes(libraries: Seq[Library]) =
      def volume(l: Library) =
        l match
          case FileLibrary(f) => Some(f -> s"/root/${f.getName}")
          case _: LibraryName | _: PackageSpec => None

      libraries.flatMap(volume)


    def installCommands(libraries: Seq[Library]): Vector[String] =
      def command(l: Library) =
        l match
          case Library.FileLibrary(f) => s"""TERM=dumb julia -e 'using Pkg; Pkg.add(path = "/root/${f.getName}")'"""
          case Library.LibraryName(name) => s"""TERM=dumb julia -e 'using Pkg; Pkg.add("$name")'"""
          case Library.PackageSpec(url, rev) =>
            val revString = rev.option.map(r => s""", rev = "$r"""").getOrElse("")
            s"""TERM=dumb julia -e 'using Pkg; Pkg.add(url = "$url"$revString)'"""

      libraries.map(command).toVector

  sealed trait Library


  def apply(
    script:                 RunnableScript,
    arguments:              OptionalArgument[String] = None,
    libraries:              Seq[Library]                       = Seq.empty,
    install:                Seq[String]                        = Seq.empty,
    installFiles:           Seq[File]                          = Seq.empty,
    prepare:                Seq[String]                        = Seq.empty,
    version:                String                             = "1.10.4",
    hostFiles:              Seq[HostFile]                      = Vector.empty,
    environmentVariables:   Seq[EnvironmentVariable]           = Vector.empty,
    errorOnReturnValue:     Boolean                            = true,
    returnValue:            OptionalArgument[Val[Int]]         = None,
    stdOut:                 OptionalArgument[Val[String]]      = None,
    stdErr:                 OptionalArgument[Val[String]]      = None,
    containerSystem:        OptionalArgument[ContainerSystem]  = None,
    clearCache:             Boolean                            = false)(using sourcecode.Name, DefinitionScope) =

  ExternalTask.build("JuliaTask"): buildParameters =>
    import buildParameters.*

    val image =
      import taskExecutionBuildContext.given
      ContainerTask.install(containerSystem, DockerImage("julia", version), install ++ Library.installCommands(Seq[Library]("JSON") ++ libraries), volumes = installFiles.map(f => f -> f.getName) ++ Library.volumes(libraries), clearCache = clearCache)

    def workDirectory = "/_workdirectory_"
    def scriptName = s"$workDirectory/_generatescript_.jl"

    val argumentsValue = arguments.map(" " + _).getOrElse("")

    val taskExecution =
      ContainerTask.execution(
        image = image,
        command = prepare ++ Seq(s"julia $scriptName $argumentsValue"),
        workDirectory = Some(workDirectory),
        errorOnReturnValue = errorOnReturnValue,
        returnValue = returnValue,
        hostFiles = hostFiles,
        environmentVariables = environmentVariables,
        stdOut = stdOut,
        stdErr = stdErr,
        config = InputOutputConfig(),
        external = external,
        info = info)

    ExternalTask.execution: p =>
      import org.json4s.jackson.JsonMethods._
      import p._
      import Mapped.noFile

      def writeInputsJSON(file: File): Unit =
        def values = noFile(mapped.inputs).map { m => (m.name, p.context(m.v)) }

        file.content = "{" + values.map { (name, value) => "\"" + name + "\": " + compact(render(toJSONValue(value))) }.mkString(",") + "}"

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


      val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".jl")
      val jsonInputs = executionContext.taskExecutionDirectory.newFile("inputs", ".json")

      val resultContext: Context =
        def inputArrayName = "_generateddata_"

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

        def containerTask =
          taskExecution.set(
            resources += (scriptFile, scriptName, true),
            resources += (jsonInputs, inputJSONName, true),
            outputFiles += (outputJSONName, outputFile),
            Mapped.files(mapped.inputs).map(m => inputFiles += (m.v, m.name, true)),
            Mapped.files(mapped.outputs).map(m => outputFiles += (m.name, m.v))
          )

        val resultContext =
          try containerTask(executionContext).from(p.context)(p.random, p.tmpDirectory, p.fileService)
          catch
            case e: Throwable => throw InternalProcessingError(s"Script content was: $scriptContent", e)

        resultContext ++ readOutputJSON(resultContext(outputFile))

      resultContext
  .set(outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)
  .withValidate: info =>
    ContainerTask.validateContainer(Vector(), environmentVariables, info.external)
