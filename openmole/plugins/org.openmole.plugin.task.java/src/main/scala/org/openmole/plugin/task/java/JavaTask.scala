package org.openmole.plugin.task.java

/*
 * Copyright (C) 2023 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.task.container.*
import org.openmole.plugin.task.external.*
import org.openmole.plugin.tool.json.*
import monocle.*
import org.openmole.core.context.ValType

object JavaTask:

  given InputOutputBuilder[JavaTask] = InputOutputBuilder(Focus[JavaTask](_.config))
  given ExternalBuilder[JavaTask] = ExternalBuilder(Focus[JavaTask](_.external))
  given InfoBuilder[JavaTask] = InfoBuilder(Focus[JavaTask](_.info))
  given MappedInputOutputBuilder[JavaTask] = MappedInputOutputBuilder(Focus[JavaTask](_.mapped))

  //def chmodDaemon = """chmod 700 /root/.local/share/scalacli/bloop/daemon"""
  //def rmSocket = "rm /root/.local/share/scalacli/bloop/daemon/socket"
  
  def scalaCLI(jvmVersion: String, javaOptions: Seq[String], fewerThreads: Boolean, server: Boolean = false) =
    def threadsOptions = if fewerThreads then Seq("-XX:+UseG1GC", "-XX:ParallelGCThreads=1", "-XX:CICompilerCount=2", "-XX:ConcGCThreads=1", "-XX:G1ConcRefinementThreads=1") else Seq()
    def allOptions = (threadsOptions ++ javaOptions).map("--java-opt " + _).mkString(" ")
    s"scala-cli run --server=$server -j $jvmVersion $allOptions"

  def dockerImage(version: String) = DockerImage("openmole/jvm", version)

  def apply(
    script: RunnableScript,
    jars: Seq[File] = Seq.empty,
    libraries: Seq[String] = Seq.empty,
    install: Seq[String] = Seq.empty,
    prepare: Seq[String] = Seq.empty,
    hostFiles: Seq[HostFile] = Vector.empty,
    environmentVariables: Seq[EnvironmentVariable] = Vector.empty,
    errorOnReturnValue: Boolean = true,
    returnValue: OptionalArgument[Val[Int]] = None,
    stdOut: OptionalArgument[Val[String]] = None,
    stdErr: OptionalArgument[Val[String]] = None,
    clearContainerCache: Boolean = false,
    jvmOptions: Seq[String] = Seq.empty,
    fewerThreads: Boolean = true,
    version: String = "17",
    containerSystem: ContainerSystem = ContainerSystem.default,
    installContainerSystem: ContainerSystem = ContainerSystem.default)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService) =

    def cacheLibraries =
      val deps = libraries.map(l => s"--dep $l").mkString(" ")
      Seq("touch _empty.sc", scalaCLI(version, jvmOptions, fewerThreads) + s" $deps  _empty.sc", "rm _empty.sc")

    new JavaTask(
      script = script,
      image = ContainerTask.install(installContainerSystem, dockerImage(version), cacheLibraries ++ install, clearCache = clearContainerCache),
      jars = jars,
      libraries = libraries,
      jvmOptions = jvmOptions,
      fewerThreads = fewerThreads,
      prepare = prepare,
      version = version,
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


case class JavaTask(
  script: RunnableScript,
  image: InstalledImage,
  jars: Seq[File],
  libraries: Seq[String],
  jvmOptions: Seq[String],
  fewerThreads: Boolean,
  version: String,
  prepare: Seq[String],
  errorOnReturnValue: Boolean,
  returnValue: Option[Val[Int]],
  stdOut: Option[Val[String]],
  stdErr: Option[Val[String]],
  hostFiles: Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  containerSystem: ContainerSystem,
  config: InputOutputConfig,
  external: External,
  info: InfoConfig,
  mapped: MappedInputOutputConfig) extends Task with ValidateTask:

  lazy val containerPoolKey = ContainerTask.newCacheKey
  override def validate = validateContainer(Vector(), environmentVariables, external)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import org.json4s.jackson.JsonMethods._
    import p._
    import Mapped.noFile

    def writeInputData(file: File): Unit =
      def values = noFile(mapped.inputs).map { m => p.context(m.v) }
      executionContext.serializerService.serialize(values.toArray, file)

    def readOutputData(file: File) = {
      val outputValues = executionContext.serializerService.deserialize[Array[Any]](file)
      (outputValues zip noFile(mapped.outputs).map(_.v)).map { case (value, v) ⇒ Variable.unsecureUntyped(v, value) }
    }

    def inputMapping(dicoName: String): String =
      noFile(mapped.inputs).zipWithIndex.map { case (m, i) ⇒ s"val ${m.name} = $dicoName($i).asInstanceOf[${ValType.toTypeString(m.v.`type`)}]" }.mkString("\n")

    def outputMapping: String =
      s"""Array[Any](${noFile(mapped.outputs).map { m ⇒ m.name }.mkString(",")})"""

    def librariesContent =
      libraries.map(l => s"//>using dep \"$l\"").mkString("\n")

    val resultContext: Context = p.newFile.withTmpFile("script", ".py") { scriptFile ⇒
      p.newFile.withTmpFile("inputs", ".bin") { inputData ⇒

        def inputArrayName = "_generateddata_"
        def scriptName = "_generatedscript_.sc"
        def inputDataName = "_inputs_.bin"
        def outputDataName = "_outputs_.bin"

        writeInputData(inputData)
        scriptFile.content =
          s"""
             |//>using dep "com.thoughtworks.xstream:xstream:1.4.20"
             |$librariesContent
             |val __serializer__ =
             |  import com.thoughtworks.xstream.*
             |  import com.thoughtworks.xstream.io.binary.*
             |  new XStream(null, new BinaryStreamDriver())
             |
             |val ${inputArrayName} =
             |  import java.io.File
             |  __serializer__.fromXML(new File("/$inputDataName")).asInstanceOf[Array[Any]]
             |
             |${inputMapping(inputArrayName)}
             |
             |${RunnableScript.content(script)}
             |
             |{
             |  import java.io.*
             |  import java.nio.file.*
             |  val mapping = ${outputMapping}
             |  val stream = Files.newOutputStream(new File("/$outputDataName").toPath)
             |  try __serializer__.toXML(mapping, stream)
             |  finally stream.close()
             |}
        """.stripMargin

        val outputFile = Val[File]("outputFile", Namespace("JavaTask"))
        val jarResources = jars.map(j => (j, s"jars/${j.getName}"))

        def jarParameter =
          if jarResources.nonEmpty
          then s"""--jars ${jarResources.map((_, n) => s"\"/$n\"").mkString(";")}"""
          else ""

        def containerTask =
          ContainerTask(
            containerSystem = containerSystem,
            image = image,
            command = prepare ++ Seq(JavaTask.scalaCLI(version, jvmOptions, fewerThreads = fewerThreads) + s""" $jarParameter $scriptName"""),
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
            containerPoolKey = containerPoolKey) set(
            resources += (scriptFile, scriptName, true),
            resources += (inputData, inputDataName, true),
            jarResources.map((j, n) => resources += (j, n, true)),
            outputFiles += (outputDataName, outputFile),
            Mapped.files(mapped.inputs).map { case m ⇒ inputFiles += (m.v, m.name, true) },
            Mapped.files(mapped.outputs).map { case m ⇒ outputFiles += (m.name, m.v) }
          )

        val resultContext = containerTask.process(executionContext).from(p.context)(p.random, p.newFile, p.fileService)
        resultContext ++ readOutputData(resultContext(outputFile))
      }
    }
    resultContext
  }




