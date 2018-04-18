package org.openmole.plugin.task.r

import monocle.macros.Lenses
import org.json4s.JsonAST.JValue
import org.openmole.core.context.{ Namespace, Variable }
import org.openmole.plugin.task.udocker._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.workspace._
import org.openmole.plugin.task.external._
import org.openmole.core.expansion._
import org.openmole.core.threadprovider._
import org.openmole.tool.hash._
import org.openmole.core.dsl._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.outputredirection._
import org.openmole.core.workflow.builder._
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.systemexec._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.container

object RTask {

  implicit def isTask: InputOutputBuilder[RTask] = InputOutputBuilder(RTask._config)
  implicit def isExternal: ExternalBuilder[RTask] = ExternalBuilder(RTask.external)
  implicit def isInfo = InfoBuilder(info)

  implicit def isBuilder = new ReturnValue[RTask] with ErrorOnReturnValue[RTask] with StdOutErr[RTask] with EnvironmentVariables[RTask] with HostFiles[RTask] with WorkDirectory[RTask] { builder ⇒
    override def returnValue = RTask.returnValue
    override def errorOnReturnValue = RTask.errorOnReturnValue
    override def stdOut = RTask.stdOut
    override def stdErr = RTask.stdErr
    override def environmentVariables = RTask.uDocker composeLens UDockerArguments.environmentVariables
    override def hostFiles = RTask.uDocker composeLens UDockerArguments.hostFiles
    override def workDirectory = RTask.uDocker composeLens UDockerArguments.workDirectory
  }

  sealed trait InstallCommand
  object InstallCommand {
    case class RLibrary(name: String) extends InstallCommand

    def toCommand(installCommands: InstallCommand) = {
      installCommands match {
        case RLibrary(name) ⇒
          //Vector(s"""R -e 'install.packages(c(${names.map(lib ⇒ '"' + s"$lib" + '"').mkString(",")}), dependencies = T)'""")
          // TODO add proxy 
          s"""R -e 'install.packages(c("$name"), dependencies = T)'"""
      }
    }

    implicit def stringToRLibrary(name: String): InstallCommand = RLibrary(name)
    def installCommands(libraries: Vector[InstallCommand]): Vector[String] = {
      libraries.map(InstallCommand.toCommand)
    }
  }

  def rImage(version: String) = DockerImage("openmole/r-base", version)

  def apply(
    script:      FromContext[String],
    install:     Seq[String]         = Seq.empty,
    libraries:   Seq[InstallCommand] = Seq.empty,
    forceUpdate: Boolean             = false
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection): RTask = {

    def version = "3.3.3"

    val installCommands =
      install ++ InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite")))

    val uDockerArguments =
      UDockerTask.createUDocker(
        rImage(version),
        install = installCommands,
        cacheInstall = true,
        forceUpdate = forceUpdate,
        mode = "P1",
        reuseContainer = true
      )

    RTask(
      script = script,
      uDockerArguments,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      _config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      rInputs = Vector.empty,
      rOutputs = Vector.empty
    )
  }

  def toJSONValue(v: Any): org.json4s.JValue = {
    import org.json4s._

    v match {
      case v: Int      ⇒ JInt(v)
      case v: Long     ⇒ JLong(v)
      case v: String   ⇒ JString(v)
      case v: Float    ⇒ JDouble(v)
      case v: Double   ⇒ JDouble(v)
      case v: Array[_] ⇒ JArray(v.map(toJSONValue).toList)
      case _           ⇒ throw new UserBadDataError(s"Value $v of type ${v.getClass} is not convertible to JSON")
    }
  }

  def jValueToVariable(jValue: JValue, v: Val[_]): Variable[_] = {
    import org.json4s._
    import shapeless._

    val caseBoolean = TypeCase[Val[Boolean]]
    val caseInt = TypeCase[Val[Int]]
    val caseLong = TypeCase[Val[Long]]
    val caseDouble = TypeCase[Val[Double]]
    val caseString = TypeCase[Val[String]]

    val caseArrayBoolean = TypeCase[Val[Array[Boolean]]]
    val caseArrayInt = TypeCase[Val[Array[Int]]]
    val caseArrayLong = TypeCase[Val[Array[Long]]]
    val caseArrayDouble = TypeCase[Val[Array[Double]]]
    val caseArrayString = TypeCase[Val[Array[String]]]

    val caseArrayArrayBoolean = TypeCase[Val[Array[Array[Boolean]]]]
    val caseArrayArrayInt = TypeCase[Val[Array[Array[Int]]]]
    val caseArrayArrayLong = TypeCase[Val[Array[Array[Long]]]]
    val caseArrayArrayDouble = TypeCase[Val[Array[Array[Double]]]]
    val caseArrayArrayString = TypeCase[Val[Array[Array[String]]]]

    def cannotConvert = throw new UserBadDataError(s"Can not convert value of type $jValue to Int for OpenMOLE variable $v.")

    def jValueToInt(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.intValue
        case jv: JInt     ⇒ jv.num.intValue
        case jv: JLong    ⇒ jv.num.intValue
        case jv: JDecimal ⇒ jv.num.intValue
        case _            ⇒ cannotConvert
      }

    def jValueToLong(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.longValue
        case jv: JInt     ⇒ jv.num.longValue
        case jv: JLong    ⇒ jv.num.longValue
        case jv: JDecimal ⇒ jv.num.longValue
        case _            ⇒ cannotConvert
      }

    def jValueToDouble(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.doubleValue
        case jv: JInt     ⇒ jv.num.doubleValue
        case jv: JLong    ⇒ jv.num.doubleValue
        case jv: JDecimal ⇒ jv.num.doubleValue
        case _            ⇒ cannotConvert
      }

    def jValueToString(jv: JValue) =
      jv match {
        case jv: JDouble  ⇒ jv.num.toString
        case jv: JInt     ⇒ jv.num.toString
        case jv: JLong    ⇒ jv.num.toString
        case jv: JDecimal ⇒ jv.num.toString
        case jv: JString  ⇒ jv.s
        case _            ⇒ cannotConvert
      }

    def jValueToBoolean(jv: JValue) =
      jv match {
        case jv: JBool ⇒ jv.value
        case _         ⇒ cannotConvert
      }

    def jValueToArray[T: Manifest](jv: JValue, convert: JValue ⇒ T) =
      jv match {
        case jv: JArray ⇒ jv.arr.map(convert).toArray[T]
        case _          ⇒ cannotConvert
      }

    (jValue, v) match {
      case (value: JArray, caseInt(v))               ⇒ Variable(v, jValueToInt(value.arr.head))
      case (value: JArray, caseLong(v))              ⇒ Variable(v, jValueToLong(value.arr.head))
      case (value: JArray, caseDouble(v))            ⇒ Variable(v, jValueToDouble(value.arr.head))
      case (value: JArray, caseString(v))            ⇒ Variable(v, jValueToString(value.arr.head))
      case (value: JArray, caseBoolean(v))           ⇒ Variable(v, jValueToBoolean(value.arr.head))

      case (value: JArray, caseArrayInt(v))          ⇒ Variable(v, jValueToArray(value, jValueToInt))
      case (value: JArray, caseArrayLong(v))         ⇒ Variable(v, jValueToArray(value, jValueToLong))
      case (value: JArray, caseArrayDouble(v))       ⇒ Variable(v, jValueToArray(value, jValueToDouble))
      case (value: JArray, caseArrayString(v))       ⇒ Variable(v, jValueToArray(value, jValueToString))
      case (value: JArray, caseArrayBoolean(v))      ⇒ Variable(v, jValueToArray(value, jValueToBoolean))

      case (value: JArray, caseArrayArrayInt(v))     ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToInt)))
      case (value: JArray, caseArrayArrayLong(v))    ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToLong)))
      case (value: JArray, caseArrayArrayDouble(v))  ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToDouble)))
      case (value: JArray, caseArrayArrayString(v))  ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToString)))
      case (value: JArray, caseArrayArrayBoolean(v)) ⇒ Variable(v, jValueToArray(value, jValueToArray(_, jValueToBoolean)))

      case _                                         ⇒ cannotConvert
    }

  }

}

@Lenses case class RTask(
  script:             FromContext[String],
  uDocker:            UDockerArguments,
  uDockerProxy:       FromContext[String],
  errorOnReturnValue: Boolean,
  returnValue:        Option[Val[Int]],
  stdOut:             Option[Val[String]],
  stdErr:             Option[Val[String]],
  _config:            InputOutputConfig,
  external:           External,
  info:               InfoConfig,
  rInputs:            Vector[(Val[_], String)], rOutputs: Vector[(String, Val[_])]) extends Task with ValidateTask {

  override def config = UDockerTask.config(_config, returnValue, stdOut, stdErr)
  override def validate = container.validateContainer(Vector(), uDocker.environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    def writeInputsJSON(file: File) = {
      def values = rInputs.map { case (v, _) ⇒ Array(context(v)) }
      file.content = compact(render(RTask.toJSONValue(values.toArray)))
    }

    def rInputMapping(arrayName: String) =
      rInputs.zipWithIndex.map { case ((_, name), i) ⇒ s"$name = $arrayName[[${i + 1}]][[1]]" }.mkString("\n")

    def rOutputMapping =
      s"""list(${rOutputs.map { case (name, _) ⇒ name }.mkString(",")})"""

    def readOutputJSON(file: File) = {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      val outputValues = parse(file.content)
      (outputValues.asInstanceOf[JArray].arr zip rOutputs.map(_._2)).map { case (jvalue, v) ⇒ RTask.jValueToVariable(jvalue, v) }
    }

    newFile.withTmpFile("script", ".R") { scriptFile ⇒
      newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒

        def inputArrayName = "generatedomarray"
        def rScriptName = "generatedomscript.R"
        def inputJSONName = "generatedominputs.json"
        def outputJSONName = "generatedomoutputs.json"

        writeInputsJSON(jsonInputs)
        scriptFile.content = s"""
          |library("jsonlite")
          |$inputArrayName = fromJSON("$inputJSONName", simplifyMatrix = FALSE)
          |${rInputMapping(inputArrayName)}
          |${script.from(p.context)(p.random, p.newFile, p.fileService)}
          |write_json($rOutputMapping, "$outputJSONName", always_decimal = TRUE)
          """.stripMargin

        val outputFile = Val[File]("outputFile", Namespace("RTask"))

        def uDockerTask =
          UDockerTask(
            uDocker, s"R --slave -f $rScriptName", errorOnReturnValue, returnValue, stdOut, stdErr, _config, external, info) set (
            resources += (scriptFile, rScriptName, true),
            resources += (jsonInputs, inputJSONName, true),
            outputFiles += (outputJSONName, outputFile)
          )

        val resultContext = uDockerTask.process(executionContext).from(context)
        resultContext ++ readOutputJSON(resultContext(outputFile))
      }
    }
  }
}