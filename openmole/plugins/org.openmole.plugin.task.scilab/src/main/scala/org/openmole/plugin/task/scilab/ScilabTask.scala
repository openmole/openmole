package org.openmole.plugin.task.scilab

import monocle.macros._
import org.openmole.core.context.ValType
import org.openmole.core.dsl._
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.expansion._
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.outputredirection.OutputRedirection
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.plugin.task.udocker._
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec._

import scala.reflect.ClassTag

object ScilabTask {

  implicit def isTask: InputOutputBuilder[ScilabTask] = InputOutputBuilder(ScilabTask._config)
  implicit def isExternal: ExternalBuilder[ScilabTask] = ExternalBuilder(ScilabTask.external)
  implicit def isInfo = InfoBuilder(info)

  implicit def isBuilder = new ReturnValue[ScilabTask] with ErrorOnReturnValue[ScilabTask] with StdOutErr[ScilabTask] with EnvironmentVariables[ScilabTask] with HostFiles[ScilabTask] with WorkDirectory[ScilabTask] { builder ⇒
    override def returnValue = ScilabTask.returnValue
    override def errorOnReturnValue = ScilabTask.errorOnReturnValue
    override def stdOut = ScilabTask.stdOut
    override def stdErr = ScilabTask.stdErr
    override def environmentVariables = ScilabTask.uDocker composeLens UDockerArguments.environmentVariables
    override def hostFiles = ScilabTask.uDocker composeLens UDockerArguments.hostFiles
    override def workDirectory = ScilabTask.uDocker composeLens UDockerArguments.workDirectory
  }

  //  sealed trait InstallCommand
  //  object InstallCommand {
  //    case class ScilabLibrary(name: String) extends InstallCommand
  //
  //        def toCommand(installCommands: InstallCommand) =
  //          installCommands match {
  //            case ScilabLibrary(name) ⇒
  //              //Vector(s"""R -e 'install.packages(c(${names.map(lib ⇒ '"' + s"$lib" + '"').mkString(",")}), dependencies = T)'""")
  //              s"""R --slave -e 'install.packages(c("$name"), dependencies = T); library("$name")'"""
  //                 }
  //
  //        implicit def stringToRLibrary(name: String): InstallCommand = RLibrary(name, None)
  //        implicit def stringCoupleToRLibrary(couple: (String, Option[String])): InstallCommand = RLibrary(couple._1, couple._2)
  //        def installCommands(libraries: Vector[InstallCommand]): Vector[String] = libraries.map(InstallCommand.toCommand)
  //
  //  }

  def scilabImage(version: String) = DockerImage("openmole/scilab", version)

  def apply(
    script: FromContext[String],
    //install:     Seq[String]         = Seq.empty,
    //libraries:   Seq[InstallCommand] = Seq.empty,
    forceUpdate: Boolean = false,
    version:     String  = "6.0.1")(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService): ScilabTask = {

    //    // add additional installation of devtools only if needed
    //    val installCommands =
    //      if (libraries.exists { case l: InstallCommand.RLibrary ⇒ l.version.isDefined }) {
    //        install ++ Seq("apt update", "apt-get -y install libssl-dev libxml2-dev libcurl4-openssl-dev libssh2-1-dev",
    //          """R --slave -e 'install.packages("devtools", dependencies = T); library(devtools);""") ++
    //          InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite", None)))
    //      }
    //      else install ++ InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite", None)))

    val uDockerArguments =
      UDockerTask.createUDocker(
        scilabImage(version),
        install = Seq.empty,
        cacheInstall = true,
        forceUpdate = forceUpdate,
        mode = "P1",
        reuseContainer = true
      )

    ScilabTask(
      script = script,
      uDockerArguments,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      _config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      scilabInputs = Vector.empty,
      scilabOutputs = Vector.empty,
      version = version
    )
  }

  def toScilab(v: Any): String = {
    v match {
      case v: Int     ⇒ v.toString
      case v: Long    ⇒ v.toString
      case v: Double  ⇒ v.toString
      case v: Boolean ⇒ if (v) "%T" else "%F"
      case v: String  ⇒ '"' + v + '"'
      case v: Array[Array[Array[_]]] ⇒
        throw new UserBadDataError(s"The array of more than 2D $v of type ${v.getClass} is not convertible to Scilab")
      case v: Array[Array[_]] ⇒
        def line(v: Array[_]) = v.map(toScilab).mkString(", ")
        "[" + v.map(line).mkString("; ") + "]"
      case v: Array[_] ⇒ "[" + v.map(toScilab).mkString(", ") + "]"
      case _ ⇒
        throw new UserBadDataError(s"Value $v of type ${v.getClass} is not convertible to Scilab")
    }
  }

  def fromScilab(s: String, v: Val[_]) = try {
    val lines = s.split("\n").dropWhile(_.trim.isEmpty)
    if (lines.isEmpty) throw new UserBadDataError(s"Value ${s} cannot be fetched in OpenMOLE variable $v")

    import org.openmole.core.context.Variable

    def toInt(s: String) = s.trim.toDouble.toInt
    def toDouble(s: String) = s.trim.replace("D", "e").toDouble
    def toLong(s: String) = s.trim.toDouble.toLong
    def toString(s: String) = s.trim
    def toBoolean(s: String) = s.trim == "T"

    def variable = v
    def fromArray[T: ClassTag](v: Val[Array[T]], fromString: String ⇒ T) = {
      val value: Array[T] = lines.head.trim.replaceAll("  *", " ").split(" ").map(fromString).toArray
      Variable(v, value)
    }

    def fromArrayArray[T: ClassTag](v: Val[Array[Array[T]]], fromString: String ⇒ T) = {
      val value: Array[Array[T]] = lines.map(_.trim.replaceAll("  *", " ").split(" ").map(fromString).toArray).toArray
      Variable(v, value)
    }

    v match {
      case Val.caseInt(v)               ⇒ Variable.unsecure(v, toInt(lines.head))
      case Val.caseDouble(v)            ⇒ Variable.unsecure(v, toDouble(lines.head))
      case Val.caseLong(v)              ⇒ Variable.unsecure(v, toLong(lines.head))
      case Val.caseString(v)            ⇒ Variable.unsecure(v, toString(lines.head))
      case Val.caseBoolean(v)           ⇒ Variable.unsecure(v, toBoolean(lines.head))

      case Val.caseArrayInt(v)          ⇒ fromArray(v, toInt)
      case Val.caseArrayDouble(v)       ⇒ fromArray(v, toDouble)
      case Val.caseArrayLong(v)         ⇒ fromArray(v, toLong)
      case Val.caseArrayString(v)       ⇒ fromArray(v, toString)
      case Val.caseArrayBoolean(v)      ⇒ fromArray(v, toBoolean)

      case Val.caseArrayArrayInt(v)     ⇒ fromArrayArray(v, toInt)
      case Val.caseArrayArrayDouble(v)  ⇒ fromArrayArray(v, toDouble)
      case Val.caseArrayArrayLong(v)    ⇒ fromArrayArray(v, toLong)
      case Val.caseArrayArrayString(v)  ⇒ fromArrayArray(v, toString)
      case Val.caseArrayArrayBoolean(v) ⇒ fromArrayArray(v, toBoolean)

      case _                            ⇒ throw new UserBadDataError(s"Value ${s} cannot be fetched in OpenMOLE variable $v")
    }
  }
  catch {
    case t: Throwable ⇒
      throw new InternalProcessingError(s"Error parsing scilab value $s to OpenMOLE variable $v", t)
  }

}

@Lenses case class ScilabTask(
  script:             FromContext[String],
  uDocker:            UDockerArguments,
  errorOnReturnValue: Boolean,
  returnValue:        Option[Val[Int]],
  stdOut:             Option[Val[String]],
  stdErr:             Option[Val[String]],
  _config:            InputOutputConfig,
  external:           External,
  info:               InfoConfig,
  scilabInputs:       Vector[(Val[_], String)],
  scilabOutputs:      Vector[(String, Val[_])],
  version:            String) extends Task with ValidateTask {

  lazy val containerPoolKey = UDockerTask.newCacheKey

  override def config = UDockerTask.config(_config, returnValue, stdOut, stdErr)
  override def validate = container.validateContainer(Vector(), uDocker.environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._

    def majorVersion = version.takeWhile(_ != '.').toInt
    def scriptName = "openmolescript.sci"

    newFile.withTmpFile("script", ".sci") { scriptFile ⇒

      def scilabInputMapping =
        scilabInputs.map { case (v, name) ⇒ s"$name = ${ScilabTask.toScilab(context(v))}" }.mkString("\n")

      def outputFileName(v: Val[_]) = s"/${v.name}.openmole"
      def outputValName(v: Val[_]) = v.withName(v.name + "File").withType[File]
      def scilabOutputMapping =
        scilabOutputs.map { case (name, v) ⇒ s"""print("${outputFileName(v)}", $name)""" }.mkString("\n")

      scriptFile.content =
        s"""
          |${if (majorVersion < 6) """errcatch(-1,"stop")""" else ""}
          |$scilabInputMapping
          |${script.from(context)}
          |${scilabOutputMapping}
          |quit
        """.stripMargin

      def launchCommand =
        if (majorVersion >= 6) s"""scilab-cli -nb -quit -f $scriptName"""
        else s"""scilab-cli -nb -f $scriptName"""

      def uDockerTask =
        UDockerTask(
          uDocker,
          launchCommand,
          errorOnReturnValue,
          returnValue,
          stdOut,
          stdErr,
          _config,
          external,
          info,
          containerPoolKey = containerPoolKey) set (
          resources += (scriptFile, scriptName, true),
          scilabOutputs.map { case (_, v) ⇒ outputFiles.+=[UDockerTask](outputFileName(v), outputValName(v)) }
        )

      val resultContext = uDockerTask.process(executionContext).from(context)
      resultContext ++ scilabOutputs.map { case (_, v) ⇒ ScilabTask.fromScilab(resultContext(outputValName(v)).content, v) }
    }

  }
}
