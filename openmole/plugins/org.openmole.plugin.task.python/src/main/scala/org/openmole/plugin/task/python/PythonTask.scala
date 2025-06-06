
package org.openmole.plugin.task.python

import monocle.Focus
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.fileservice.FileService
import org.openmole.core.argument.OptionalArgument
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.setter._
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.core.json.*
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.plugin.task.container

object PythonTask:
  def dockerImage(version: String) = DockerImage("python", version)

  def installCommands(install: Seq[String], libraries: Seq[String], major: Int): Vector[String] =
    // need to install pip2 in case of python 2
    val effintsall =
      install ++ (
        if major == 2
        then Seq("curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py","python2 get-pip.py")
        else Seq.empty
      )

    val pipValue = s"""pip$major install --prefer-binary"""

    (effintsall ++ libraries.map(l => s"$pipValue $l")).toVector

  def apply(
    script:                 RunnableScript,
    arguments:              OptionalArgument[String] = None,
    image:                  ContainerImage                     = "openmole/python:3.13.3",
    libraries:              Seq[String]                        = Seq.empty,
    install:                Seq[String]                        = Seq.empty,
    prepare:                Seq[String]                        = Seq.empty,
    hostFiles:              Seq[HostFile]                      = Vector.empty,
    environmentVariables:   Seq[EnvironmentVariable]           = Vector.empty,
    errorOnReturnValue:     Boolean                            = true,
    returnValue:            OptionalArgument[Val[Int]]         = None,
    stdOut:                 OptionalArgument[Val[String]]      = None,
    stdErr:                 OptionalArgument[Val[String]]      = None,
    containerSystem:        OptionalArgument[ContainerSystem]  = None)(using sourcecode.Name, DefinitionScope) =

    
    ExternalTask.build("PythonTask"): buildParamters =>
      import buildParamters.*

      val major =
        image match
          case image: DockerImage if image.tag.startsWith("2") => 2
          case _ => 3
        
      val containerImage =
        import taskExecutionBuildContext.given
        ContainerTask.install(containerSystem, image, installCommands(install, libraries, major))

      def workDirectory = "/_workdirectory_"
      
      def scriptPath = s"$workDirectory/_generatescript_.py"
      
      val argumentsValue = arguments.map(" " + _).getOrElse("")
      
      val taskExecution =
        ContainerTask.execution(
          image = containerImage,
          command = prepare ++ Seq(s"python${major.toString} $scriptPath" + argumentsValue),
          workDirectory = Some(workDirectory),
          errorOnReturnValue = errorOnReturnValue,
          returnValue = returnValue,
          environmentVariables = environmentVariables,
          hostFiles = hostFiles,
          stdOut = stdOut,
          stdErr = stdErr,
          external = external,
          config = config,
          info = info)

      ExternalTask.execution: p =>
        import org.json4s.jackson.JsonMethods._
        import p._
        import Mapped.noFile
      
        def writeInputsJSON(file: File): Unit =
          def values = noFile(mapped.inputs).map { m => p.context(m.v) }
      
          file.content = compact(render(toJSONValue(values.toArray)))
      
        def readOutputJSON(file: File) =
          import org.json4s._
          import org.json4s.jackson.JsonMethods._
          val outputValues = parse(file.content)
          (outputValues.asInstanceOf[JArray].arr zip noFile(mapped.outputs).map(_.v)).map { (jvalue, v) => jValueToVariable(jvalue, v, unwrapArrays = true) }
      
        def inputMapping(dicoName: String): String =
          noFile(mapped.inputs)
            .zipWithIndex
            .map { (m, i) => s"${m.name} = $dicoName[${i}]" }
            .mkString("\n")
      
        def outputMapping: String =
          s"""[${noFile(mapped.outputs).map { m => m.name }.mkString(",")}]"""
      
        val resultContext: Context =
          val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".py")
          val jsonInputFile = executionContext.taskExecutionDirectory.newFile("input", ".json")
      
          def inputArrayName = "_generateddata_"
      
          def inputJSONPath = s"$workDirectory/_inputs_.json"
      
          def outputJSONPath = s"$workDirectory/_outputs_.json"
      
          def alignedUserCode =
            val source = RunnableScript.content(script)
            val lines = source.split('\n')
            if lines.nonEmpty
            then
              val minSpace = lines.filter(_.trim.nonEmpty).map(_.takeWhile(_ == ' ').length).min
              if minSpace > 0
              then lines.map(_.drop(minSpace)).mkString("\n")
              else source
            else source
      
          writeInputsJSON(jsonInputFile)
          scriptFile.content =
            s"""
               |import json
               |$inputArrayName = json.load(open('$inputJSONPath'))
               |${inputMapping(inputArrayName)}
               |${alignedUserCode}
               |json.dump($outputMapping, open('$outputJSONPath','w'))
            """.stripMargin
      
          val outputFile = Val[File]("outputFile", Namespace("PythonTask"))
      
          def containerTask =
            taskExecution.set(
              resources += (scriptFile, scriptPath, true),
              resources += (jsonInputFile, inputJSONPath, true),
              outputFiles += (outputJSONPath, outputFile),
              Mapped.files(mapped.inputs).map(m => inputFiles += (m.v, m.name, true)),
              Mapped.files(mapped.outputs).map(m => outputFiles += (m.name, m.v))
            )
      
          val resultContext = containerTask(executionContext).from(p.context)(p.random, p.tmpDirectory, p.fileService)
          resultContext ++ readOutputJSON(resultContext(outputFile))
      
        resultContext
    .set (outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)
    .withValidate: info =>
      ContainerTask.validateContainer(Vector(), environmentVariables, info.external)

