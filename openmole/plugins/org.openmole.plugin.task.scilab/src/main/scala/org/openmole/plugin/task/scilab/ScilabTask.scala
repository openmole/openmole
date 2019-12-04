package org.openmole.plugin.task.scilab

import monocle.macros._
import org.openmole.core.dsl._
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.expansion._
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.tool.outputredirection.OutputRedirection

import scala.annotation.tailrec
import scala.reflect.ClassTag

object ScilabTask {

  implicit def isTask: InputOutputBuilder[ScilabTask] = InputOutputBuilder(ScilabTask.config)
  implicit def isExternal: ExternalBuilder[ScilabTask] = ExternalBuilder(ScilabTask.external)
  implicit def isInfo = InfoBuilder(info)
  implicit def isMapped = MappedInputOutputBuilder(ScilabTask.mapped)

  def scilabImage(version: String) = DockerImage("openmole/scilab", version)

  def apply(
    script:               RunnableScript,
    install:              Seq[String]                   = Seq.empty,
    forceUpdate:          Boolean                       = false,
    version:              String                        = "6.0.2",
    errorOnReturnValue:   Boolean                       = true,
    returnValue:          OptionalArgument[Val[Int]]    = None,
    stdOut:               OptionalArgument[Val[String]] = None,
    stdErr:               OptionalArgument[Val[String]] = None,
    environmentVariables: Seq[EnvironmentVariable]      = Vector.empty,
    hostFiles:            Seq[HostFile]                 = Vector.empty,
    workDirectory:        OptionalArgument[String]      = None,
    containerSystem:      ContainerSystem               = Proot())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: TmpDirectory, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService, serializerService: SerializerService): ScilabTask = {

    ScilabTask(
      script = script,
      image = ContainerTask.prepare(containerSystem, scilabImage(version), install),
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
      mapped = MappedInputOutputConfig(),
      version = version
    ) set (
        outputs += (Seq(returnValue.option, stdOut.option, stdErr.option).flatten: _*)
      )
  }

  /**
   * transpose and stringify a multidimensional array
   * @param v
   * @return
   */
  def multiArrayScilab(v: Any): String = {
    // flatten the array after multidimensional transposition
    def recTranspose(v: Any): Seq[_] = {
      v match {
        case v: Array[Array[Array[_]]] ⇒ v.map { a ⇒ recTranspose(a) }.toSeq.transpose.flatten
        case v: Array[Array[_]]        ⇒ v.map { _.toSeq }.toSeq.transpose.flatten
      }
    }
    def getDimensions(v: Any): Seq[Int] = {
      @tailrec def getdims(v: Any, dims: Seq[Int]): Seq[Int] = {
        v match {
          case v: Array[Array[_]] ⇒ getdims(v(0), dims ++ Seq(v.length))
          case v: Array[_]        ⇒ dims ++ Seq(v.length)
        }
      }
      getdims(v, Seq.empty)
    }
    val scilabVals = recTranspose(v).map { vv ⇒ toScilab(vv) }
    val dimensions = getDimensions(v)
    // scilab syntax for hypermat
    // M = hypermat([2 3 2 2],data) with data being flat column vector
    // NOTE : going to string may be too large for very large arrays ? would need a proper serialization ?
    "hypermat([" + dimensions.mkString(" ") + "],[" + scilabVals.mkString(";") + "])"
  }

  def toScilab(v: Any): String = {
    v match {
      case v: Int                    ⇒ v.toString
      case v: Long                   ⇒ v.toString
      case v: Double                 ⇒ v.toString
      case v: Boolean                ⇒ if (v) "%T" else "%F"
      case v: String                 ⇒ '"' + v + '"'
      case v: Array[Array[Array[_]]] ⇒ multiArrayScilab(v)
      //multiArrayScilab(v.map { _.map { _.toSeq }.toSeq }.toSeq)
      //throw new UserBadDataError(s"The array of more than 2D $v of type ${v.getClass} is not convertible to Scilab")
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
  mapped:               MappedInputOutputConfig,
  version:              String) extends Task with ValidateTask {

  lazy val containerPoolKey = ContainerTask.newCacheKey

  override def validate = container.validateContainer(Vector(), environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._

    def majorVersion = version.takeWhile(_ != '.').toInt
    def scriptName = "openmolescript.sci"

    newFile.withTmpFile("script", ".sci") { scriptFile ⇒

      def scilabInputMapping =
        mapped.inputs.map { m ⇒ s"${m.name} = ${ScilabTask.toScilab(context(m.v))}" }.mkString("\n")

      def outputFileName(v: Val[_]) = s"/${v.name}.openmole"
      def outputValName(v: Val[_]) = v.withName(v.name + "File").withType[File]
      def scilabOutputMapping =
        mapped.outputs.map { m ⇒ s"""print("${outputFileName(m.v)}", ${m.name})""" }.mkString("\n")

      scriptFile.content =
        s"""
          |${if (majorVersion < 6) """errcatch(-1,"stop")""" else ""}
          |$scilabInputMapping
          |${RunnableScript.content(script)}
          |${scilabOutputMapping}
          |quit
        """.stripMargin

      def launchCommand =
        if (majorVersion >= 6) s"""scilab-cli -nb -quit -f $scriptName"""
        else s"""scilab-cli -nb -f $scriptName"""

      def containerTask =
        ContainerTask(
          containerSystem = containerSystem,
          image = image,
          command = launchCommand,
          workDirectory = None,
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
          resources += (scriptFile, scriptName, true),
          mapped.outputs.map { m ⇒ outputFiles.+=[ContainerTask](outputFileName(m.v), outputValName(m.v)) }
        )

      val resultContext = containerTask.process(executionContext).from(context)
      resultContext ++ mapped.outputs.map { m ⇒ ScilabTask.fromScilab(resultContext(outputValName(m.v)).content, m.v) }
    }

  }
}
