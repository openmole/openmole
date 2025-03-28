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
import org.openmole.core.json.*
import monocle.*
import org.openmole.core.context.ValType

object JavaTask:

  def scalaCLI(jvmVersion: String, javaOptions: Seq[String], fewerThreads: Boolean, server: Boolean = false, offline: Boolean = false) =
    def threadsOptions = if fewerThreads then JavaConfiguration.fewerThreadsParameters else Seq()
    def allOptions = (threadsOptions ++ javaOptions).map("--java-opt " + _).mkString(" ")
    def offlineOption = if offline then "--suppress-experimental-warning --power --offline" else ""
    s"scala-cli run $offlineOption --server=$server -j $jvmVersion $allOptions"

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
    version: String = "21.2",
    jvmVersion: String = "21",
    containerSystem: OptionalArgument[ContainerSystem] = None)(using sourcecode.Name, DefinitionScope) =

    ExternalTask.build("JavaTask"): buildContext =>
      import buildContext.*

      def cacheLibraries =
        val deps = libraries.map(l => s"--dep $l").mkString(" ")
        Seq("cd /tmp", "touch _empty.sc", scalaCLI(jvmVersion, jvmOptions, fewerThreads) + s" $deps  _empty.sc", "rm _empty.sc")

      val image =
        import taskExecutionBuildContext.given
        ContainerTask.install(containerSystem, dockerImage(version), cacheLibraries ++ install, clearCache = clearContainerCache)

      def workspaceName = "/_workspace_"
      def scriptName = "_generatedscript_.sc"

      val jarResources = jars.map(j => (j, s"/jars/${j.getName}"))

      def jarParameter =
        if jarResources.nonEmpty
        then jarResources.map(j => s"""--jar \"${j._2}\"""").mkString(" ")
        else ""

      val taskExecution = ContainerTask.execution(
        image = image,
        command = prepare ++ Seq(JavaTask.scalaCLI(jvmVersion, jvmOptions, fewerThreads = fewerThreads, offline = true) + s""" $jarParameter $scriptName"""),
        workDirectory = Some(workspaceName),
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
        import org.json4s.jackson.JsonMethods.*
        import p.*
        import Mapped.noFile

        def writeInputData(file: File): Unit =
          def values = noFile(mapped.inputs).map { m => p.context(m.v) }

          executionContext.serializerService.serialize(values.toArray, file)

        def readOutputData(file: File) =
          val outputValues = executionContext.serializerService.deserialize[Array[Any]](file)
          (outputValues zip noFile(mapped.outputs).map(_.v)).map { case (value, v) => Variable.unsecureUntyped(v, value) }

        def inputMapping(dicoName: String): String =
          noFile(mapped.inputs).zipWithIndex.map { case (m, i) => s"val ${m.name} = $dicoName($i).asInstanceOf[${ValType.toTypeString(m.v.`type`)}]" }.mkString("\n")

        def outputMapping: String =
          s"""Array[Any](${noFile(mapped.outputs).map { m => m.name }.mkString(",")})"""

        def librariesContent =
          libraries.map(l => s"//>using dep \"$l\"").mkString("\n")

        val resultContext: Context =
          def inputArrayName = "_generateddata_"

          val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".sc")

          def inputDataName = s"$workspaceName/_inputs_.bin"

          val inputData = executionContext.taskExecutionDirectory.newFile("inputs", ".bin")

          def outputDataName = s"$workspaceName/_outputs_.bin"

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
               |  __serializer__.fromXML(new File("$inputDataName")).asInstanceOf[Array[Any]]
               |
               |${inputMapping(inputArrayName)}
               |
               |${RunnableScript.content(script)}
               |
               |{
               |  import java.io.*
               |  import java.nio.file.*
               |  val mapping = ${outputMapping}
               |  val stream = Files.newOutputStream(new File("$outputDataName").toPath)
               |  try __serializer__.toXML(mapping, stream)
               |  finally stream.close()
               |}
          """.stripMargin

          val outputFile = Val[File]("outputFile", Namespace("JavaTask"))

          def containerTask =
            taskExecution.set(
              resources += (scriptFile, scriptName, true),
              resources += (inputData, inputDataName, true),
              jarResources.map((j, n) => resources += (j, n, true)),
              outputFiles += (outputDataName, outputFile),
              Mapped.files(mapped.inputs).map(m => inputFiles += (m.v, m.name, true)),
              Mapped.files(mapped.outputs).map(m => outputFiles += (m.name, m.v))
            )

          val resultContext = containerTask(executionContext).from(p.context)(p.random, p.tmpDirectory, p.fileService)
          resultContext ++ readOutputData(resultContext(outputFile))

        resultContext

  .set(outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)
  .withValidate: info =>
    ContainerTask.validateContainer(Vector(), environmentVariables, info.external)

