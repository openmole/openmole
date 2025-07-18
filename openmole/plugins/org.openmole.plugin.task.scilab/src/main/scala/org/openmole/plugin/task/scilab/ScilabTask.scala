package org.openmole.plugin.task.scilab

import monocle.Focus
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.argument._
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.setter._
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.tool.outputredirection.OutputRedirection

import scala.annotation.tailrec
import scala.reflect.ClassTag

object ScilabTask:

  def scilabImage(version: String) = DockerImage("openmole/scilab", version)

  def apply(
    script:                 RunnableScript,
    install:                Seq[String]                         = Seq.empty,
    prepare:                Seq[String]                         = Seq.empty,
    version:                String                              = "2025.0.0",
    errorOnReturnValue:     Boolean                             = true,
    returnValue:            OptionalArgument[Val[Int]]          = None,
    stdOut:                 OptionalArgument[Val[String]]       = None,
    stdErr:                 OptionalArgument[Val[String]]       = None,
    environmentVariables:   Seq[EnvironmentVariable]            = Vector.empty,
    hostFiles:              Seq[HostFile]                       = Vector.empty,
    containerSystem:        OptionalArgument[ContainerSystem]   = None)(using sourcecode.Name, DefinitionScope) =

    ExternalTask.build("ScilabTask"): buildParameters =>
      import buildParameters.*

      val image =
        import taskExecutionBuildContext.given
        ContainerTask.install(containerSystem, scilabImage(version), install)

      def workDirectory = "/_workdirectory_"

      def scriptName = s"$workDirectory/openmolescript.sci"

      def majorVersion = version.takeWhile(_ != '.').toInt

      def launchCommand =
        if majorVersion >= 6
        then s"""scilab-cli -nwni -nb -quit -f $scriptName"""
        else s"""scilab-cli -nb -f $scriptName"""

      val taskExecution =
        ContainerTask.execution(
          image = image,
          command = prepare ++ Seq(launchCommand),
          workDirectory = Some(workDirectory),
          errorOnReturnValue = errorOnReturnValue,
          returnValue = returnValue,
          hostFiles = hostFiles,
          environmentVariables = environmentVariables,
          stdOut = stdOut,
          stdErr = stdErr,
          config = config,
          external = external,
          info = info)

      ExternalTask.execution: p =>

        import p.*

        val scriptFile = executionContext.taskExecutionDirectory.newFile("script", ".sci")

        def scilabInputMapping =
          mapped.inputs.map { m => s"${m.name} = ${ScilabTask.toScilab(context(m.v))}" }.mkString("\n")

        def outputFileName(v: Val[?]) = s"$workDirectory/${v.name}.openmole"

        def outputValName(v: Val[?]) = v.withName(v.name + "File").withType[File]

        def scilabOutputMapping =
          (Seq("lines(0, 1000000000)") ++ mapped.outputs.map { m => s"""print("${outputFileName(m.v)}", ${m.name})""" }).mkString("\n")

        scriptFile.content =
          s"""
             |${if (majorVersion < 6) """errcatch(-1,"stop")""" else ""}
             |$scilabInputMapping
             |${RunnableScript.content(script)}
             |${scilabOutputMapping}
             |quit
            """.stripMargin

        def containerTask =
          taskExecution.set(
            resources += (scriptFile, scriptName, true),
            mapped.outputs.map(m => outputFiles += (outputFileName(m.v), outputValName(m.v)))
          )

        val resultContext = containerTask(executionContext).from(context)
        resultContext ++ mapped.outputs.map { m => ScilabTask.fromScilab(resultContext(outputValName(m.v)).content, m.v) }

  .set(outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)
  .withValidate: info =>
    ContainerTask.validateContainer(Vector(), environmentVariables, info.external)


  /**
   * transpose and stringify a multidimensional array
   * @param v
   * @return
   */
  def multiArrayScilab(v: Any): String =
    // flatten the array after multidimensional transposition
    def recTranspose(v: Any): Seq[?] =
      v match
        case v: Array[Array[Array[?]]] => v.map { a => recTranspose(a) }.toSeq.transpose.flatten
        case v: Array[Array[?]]        => v.map { _.toSeq }.toSeq.transpose.flatten

    def getDimensions(v: Any): Seq[Int] =
      @tailrec def getdims(v: Any, dims: Seq[Int]): Seq[Int] =
        v match
          case v: Array[Array[?]] => getdims(v(0), dims ++ Seq(v.length))
          case v: Array[?]        => dims ++ Seq(v.length)
      getdims(v, Seq.empty)

    val scilabVals = recTranspose(v).map { vv => toScilab(vv) }
    val dimensions = getDimensions(v)
    // scilab syntax for hypermat
    // M = hypermat([2 3 2 2],data) with data being flat column vector
    // NOTE : going to string may be too large for very large arrays ? would need a proper serialization ?
    "hypermat([" + dimensions.mkString(" ") + "],[" + scilabVals.mkString(";") + "])"

  def toScilab(v: Any): String =
    v match
      case v: Int                    => v.toString
      case v: Long                   => v.toString
      case v: Double                 => v.toString
      case v: Boolean                => if (v) "%T" else "%F"
      case v: String                 => '"' + v + '"'
      case v: Array[Array[Array[?]]] => multiArrayScilab(v)
      //multiArrayScilab(v.map { _.map { _.toSeq }.toSeq }.toSeq)
      //throw new UserBadDataError(s"The array of more than 2D $v of type ${v.getClass} is not convertible to Scilab")
      case v: Array[Array[?]] =>
        def line(v: Array[?]) = v.map(toScilab).mkString(", ")
        "[" + v.map(line).mkString("; ") + "]"
      case v: Array[?] => "[" + v.map(toScilab).mkString(", ") + "]"
      case _ =>
        throw new UserBadDataError(s"Value $v of type ${v.getClass} is not convertible to Scilab")

  def fromScilab(s: String, v: Val[?]) =
    try
      val lines = s.split("\n").dropWhile(_.trim.isEmpty)
      if (lines.isEmpty) throw new UserBadDataError(s"Value ${s} cannot be fetched in OpenMOLE variable $v")

      import org.openmole.core.context.Variable

      def toInt(s: String) = s.trim.toDouble.toInt
      def toDouble(s: String) = s.trim.replace("D", "e").toDouble
      def toLong(s: String) = s.trim.toDouble.toLong
      def toString(s: String) = s.trim
      def toBoolean(s: String) = s.trim == "T"

      def variable = v
      def fromArray[T: ClassTag](v: Val[Array[T]], fromString: String => T) =
        val value: Array[T] =
          if lines.head.trim == "[]"
          then Array.empty
          else lines.head.trim.replaceAll("  *", " ").split(" ").map(fromString).toArray
        Variable(v, value)

      def fromArrayArray[T: ClassTag](v: Val[Array[Array[T]]], fromString: String => T) =
        val value: Array[Array[T]] =
          if lines.head.trim == "[]"
          then Array.empty
          else lines.map(_.trim.replaceAll("  *", " ").split(" ").map(fromString).toArray).toArray
        Variable(v, value)

      v match
        case Val.caseInt(v)               => Variable.unsecure(v, toInt(lines.head))
        case Val.caseDouble(v)            => Variable.unsecure(v, toDouble(lines.head))
        case Val.caseLong(v)              => Variable.unsecure(v, toLong(lines.head))
        case Val.caseString(v)            => Variable.unsecure(v, toString(lines.head))
        case Val.caseBoolean(v)           => Variable.unsecure(v, toBoolean(lines.head))

        case Val.caseArrayInt(v)          => fromArray(v, toInt)
        case Val.caseArrayDouble(v)       => fromArray(v, toDouble)
        case Val.caseArrayLong(v)         => fromArray(v, toLong)
        case Val.caseArrayString(v)       => fromArray(v, toString)
        case Val.caseArrayBoolean(v)      => fromArray(v, toBoolean)

        case Val.caseArrayArrayInt(v)     => fromArrayArray(v, toInt)
        case Val.caseArrayArrayDouble(v)  => fromArrayArray(v, toDouble)
        case Val.caseArrayArrayLong(v)    => fromArrayArray(v, toLong)
        case Val.caseArrayArrayString(v)  => fromArrayArray(v, toString)
        case Val.caseArrayArrayBoolean(v) => fromArrayArray(v, toBoolean)

        case _                            => throw new UserBadDataError(s"Value ${s} cannot be fetched in OpenMOLE variable $v")
    catch
      case t: Throwable =>
        throw new InternalProcessingError(s"Error parsing scilab value $s to OpenMOLE variable $v", t)


