
package org.openmole.plugin.task.python

import org.openmole.core.context.{Val, Context}
import org.openmole.core.workflow.tools.OptionalArgument
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.{DefinitionScope, InfoConfig, InputOutputConfig, MappedInputOutputConfig}
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workspace.{NewFile, Workspace}
import org.openmole.plugin.task.container.HostFile
import org.openmole.plugin.task.external.{External, outputFiles, resources}
import org.openmole.plugin.task.udocker.{DockerImage, UDockerTask}
import org.openmole.plugin.tool.json._
import org.openmole.tool.outputredirection.OutputRedirection

object PythonTask {


  //  distinct images for python2 and python3 ? => pip for python3 only on dockerhub - provisory python3 only for tests
  // FIXME openmole python docker image
  //def dockerImage(major: Int) = DockerImage("openmole/python-"+major)
  def dockerImage(major: Int) = DockerImage("python")

  def installCommands(install: Seq[String],libraries: Seq[String]): Vector[String] =
    (install ++ libraries.map{ l => "pip install "+l}).toVector

  def apply(
             script: FromContext[String],
             major: Int,
             libraries: Seq[String] = Seq.empty,
             install:              Seq[String]                        = Seq.empty,
             forceUpdate:          Boolean                            = false,
             workDirectory:        OptionalArgument[String]           = None,
             hostFiles:            Seq[HostFile]                      = Vector.empty,
             environmentVariables: Seq[(String, FromContext[String])] = Vector.empty,
             mapped:             MappedInputOutputConfig = MappedInputOutputConfig(),
             errorOnReturnValue: Boolean = true,
             returnValue:        Option[Val[Int]] = None,
             stdOut:             Option[Val[String]] = None,
             stdErr:             Option[Val[String]] = None,
           )(implicit
             name: sourcecode.Name,
             definitionScope: DefinitionScope,
             workspace: Workspace,
             preference: Preference,
             threadProvider: ThreadProvider,
             outputRedirection: OutputRedirection,
             networkService: NetworkService,
             taskExecutionContext: TaskExecutionContext
  ) = Task("PythonTask") {
    p: org.openmole.core.workflow.task.FromContextTask.Parameters =>
      import p._
      import org.json4s.jackson.JsonMethods._

      lazy val containerPoolKey = UDockerTask.newCacheKey

      val udocker =
        UDockerTask.createUDocker(
          dockerImage(major),
          install = installCommands(install,libraries),
          cacheInstall = true,
          forceUpdate = forceUpdate,
          mode = "P1",
          reuseContainer = true,
          hostFiles = hostFiles,
          workDirectory = workDirectory
        )(p.newFile, preference, threadProvider, workspace, p.fileService, outputRedirection, networkService)/*.copy(
          environmentVariables = environmentVariables.toVector,
          hostFiles = hostFiles.toVector,
          workDirectory = workDirectory)*/

      def writeInputsJSON(file: File) = {
        def values = mapped.inputs.map { m ⇒ Array(context(m.v)) }
        file.content = compact(render(toJSONValue(values.toArray)))
      }

      def readOutputJSON(file: File) = {
        import org.json4s._
        import org.json4s.jackson.JsonMethods._
        val outputValues = parse(file.content)
        (outputValues.asInstanceOf[JArray].arr zip mapped.outputs.map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v) }
      }

      def inputMapping(dicoName: String): String =
        mapped.inputs.map { m ⇒ s"${m.name} = $dicoName['${m.name}']" }.mkString("\n")

      def outputMapping: String = s"""{${mapped.outputs.map { m => "'"+m.name+"' : "+m.v }.mkString(",")}}"""

      val resultContext: Context = p.newFile.withTmpFile("script", ".py") { scriptFile ⇒
        p.newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒

          def inputArrayName = "data"
          def scriptName = "generatescript.py"
          def inputJSONName = "generatedinputs.json"
          def outputJSONName = "outputs.json"

          writeInputsJSON(jsonInputs)
          scriptFile.content =
            s"""
               |import json
               |$inputArrayName = json.load(open('/$inputJSONName'))
               |${inputMapping(inputArrayName)}
               |${script.from(p.context)(p.random, p.newFile, p.fileService)}
               |json.dump($outputMapping, open('/$outputJSONName','w'))
          """.stripMargin

          val outputFile = Val[File]("outputFile", Namespace("PythonTask"))

          def uDockerTask =
            UDockerTask(
              udocker, s"python $scriptName",
              errorOnReturnValue,
              returnValue,
              stdOut,
              stdErr,
              InputOutputConfig(),
              External(),
              InfoConfig(),
              containerPoolKey = containerPoolKey) set(
              resources += (scriptFile, scriptName, true),
              resources += (jsonInputs, inputJSONName, true),
              outputFiles += (outputJSONName, outputFile)
            )

          val resultContext: org.openmole.core.context.Context = uDockerTask.process(taskExecutionContext).from(context)
          resultContext ++ readOutputJSON(resultContext(outputFile))
        }
      }
      resultContext
  } validate {_ => Seq.empty }


}

