
package org.openmole.plugin.task.python

import org.openmole.core.context.{Context, Val}
import org.openmole.core.workflow.tools.OptionalArgument
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workspace.{NewFile, Workspace}
import org.openmole.plugin.task.container.HostFile
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.udocker._
import org.openmole.plugin.tool.json._
import org.openmole.tool.outputredirection.OutputRedirection

object PythonTask {


  //  distinct images for python2 and python3 ? => pip for python3 only on dockerhub - provisory python3 only for tests
  // FIXME openmole python docker image - major is not taken into account for now
  //def dockerImage(major: Int) = DockerImage("openmole/python-"+major)
  def dockerImage(major: Int) = DockerImage("python")

  def installCommands(install: Seq[String],libraries: Seq[String]): Vector[String] =
    (install ++ libraries.map{ l => "pip install "+l}).toVector

  def apply(
             script: RunnableScript,
             major: Int,
             libraries: Seq[String] = Seq.empty,
             install:              Seq[String]                        = Seq.empty,
             forceUpdate:          Boolean                            = false,
             workDirectory:        OptionalArgument[String]           = None,
             hostFiles:            Seq[HostFile]                      = Vector.empty,
             environmentVariables: Seq[(String, FromContext[String])] = Vector.empty,
             errorOnReturnValue: Boolean = true,
             returnValue:        Option[Val[Int]] = None,
             stdOut:             Option[Val[String]] = None,
             stdErr:             Option[Val[String]] = None,
           )(implicit name: sourcecode.Name, definitionScope: DefinitionScope,newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService) = {


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
      ).copy(
        environmentVariables = environmentVariables.toVector
      )



    Task("PythonTask") {
      p: org.openmole.core.workflow.task.FromContextTask.Parameters =>
        import p._
        import org.json4s.jackson.JsonMethods._

        def writeInputsJSON(file: File): Unit = {
          def values = mapped.inputs.map { m ⇒ p.context(m.v) }
          file.content = compact(render(toJSONValue(values.toArray)))
          //file.content = "{"+mapped.inputs.map {mio => s"'${mio.name}' :"+toJSONValue(p.context(mio.v))}.mkString(",")+"}"
        }

        def readOutputJSON(file: File) = {
          import org.json4s._
          import org.json4s.jackson.JsonMethods._
          val outputValues = parse(file.content)
          (outputValues.asInstanceOf[JArray].arr zip mapped.outputs.map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v) }
        }

        def inputMapping(dicoName: String): String =
          //mapped.inputs.map { m ⇒ s"${m.name} = $dicoName['${m.name}']" }.mkString("\n")
          mapped.inputs.zipWithIndex.map { case (m,i) ⇒ s"${m.name} = $dicoName[${i}]" }.mkString("\n")

        def outputMapping: String = //s"""{${mapped.outputs.map { m => "'"+m.name+"' : "+m.name }.mkString(",")}}"""
          s"""[${mapped.outputs.map { m => m.name}.mkString(",")}]"""

        // RunnableScript replaces this more properly
        //val userScript = script.from(p.context)(p.random, p.newFile, p.fileService)
        //val scriptString = if (userScript.endsWith(".py")) File(userScript).content else userScript


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
                 |f = open('/$inputJSONName','r')
                 |print(f.readlines())
                 |$inputArrayName = json.load(open('/$inputJSONName'))
                 |${inputMapping(inputArrayName)}
                 |${script.script}
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


            val resultContext: org.openmole.core.context.Context = uDockerTask.process(p.executionContext).from(p.context)(p.random,p.newFile,p.fileService)
            resultContext ++ readOutputJSON(resultContext(outputFile))
          }
        }
        resultContext
    } validate {_ => Seq.empty }
  }


}

