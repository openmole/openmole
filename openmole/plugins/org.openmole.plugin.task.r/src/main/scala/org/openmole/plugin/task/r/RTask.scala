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

  implicit def isBuilder = new ReturnValue[RTask] with ErrorOnReturnValue[RTask] with StdOutErr[RTask] with EnvironmentVariables[RTask] with HostFiles[RTask] with ReuseContainer[RTask] with WorkDirectory[RTask] with UDockerUser[RTask] { builder ⇒
    override def returnValue = RTask.returnValue
    override def errorOnReturnValue = RTask.errorOnReturnValue
    override def stdOut = RTask.stdOut
    override def stdErr = RTask.stdErr
    override def environmentVariables = RTask.uDocker composeLens UDockerArguments.environmentVariables
    override def hostFiles = RTask.uDocker composeLens UDockerArguments.hostFiles
    override def reuseContainer = RTask.uDocker composeLens UDockerArguments.reuseContainer
    override def workDirectory = RTask.uDocker composeLens UDockerArguments.workDirectory
    override def uDockerUser = RTask.uDocker composeLens UDockerArguments.uDockerUser
  }

  sealed trait InstallCommand
  object InstallCommand {
    case class RLibrary(name: String) extends InstallCommand

    def toCommand(installCommands: InstallCommand) = {
      installCommands match {
        case RLibrary(name) ⇒
          //Vector(s"""R -e 'install.packages(c(${names.map(lib ⇒ '"' + s"$lib" + '"').mkString(",")}), dependencies = T)'""")
          s"""R -e 'install.packages(c("$name"), dependencies = T)'"""
      }
    }

    implicit def stringToRLibrary(name: String): InstallCommand = RLibrary(name)
    def installCommands(libraries: Vector[InstallCommand]): Vector[String] = libraries.map(InstallCommand.toCommand)
  }

  def rImage(version: String) = DockerImage("r-base", version)

  def apply(
    script:      FromContext[String],
    install:     Seq[InstallCommand] = Seq.empty,
    version:     String              = "3.4.3",
    forceUpdate: Boolean             = false
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection): RTask = {

    val installCommands = InstallCommand.installCommands(install.toVector ++ Seq(InstallCommand.RLibrary("jsonlite")))
    val cacheKey: Option[String] =
      Some((Seq(rImage(version).image, rImage(version).tag) ++ installCommands).mkString("\n").hash().toString)

    val uDockerArguments =
      UDockerTask.createUDocker(
        rImage(version),
        installCommands = installCommands,
        cachedKey = OptionalArgument(cacheKey),
        forceUpdate = forceUpdate
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

  def jValueToVariable(jvalue: JValue, v: Val[_]): Variable[_] = {
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

    (jvalue, v) match {
      case (value: JDouble, caseInt(v))         ⇒ Variable(v, value.num.intValue)
      case (value: JDouble, caseLong(v))        ⇒ Variable(v, value.num.longValue)
      case (value: JDouble, caseDouble(v))      ⇒ Variable(v, value.num)
      case (value: JString, caseString(v))      ⇒ Variable(v, value.s)
      case (value: JBool, caseBoolean(v))       ⇒ Variable(v, value.value)

      case (value: JArray, caseArrayInt(v))     ⇒ Variable(v, value.arr.map(_.asInstanceOf[JDouble].num.intValue).toArray[Int])
      case (value: JArray, caseArrayLong(v))    ⇒ Variable(v, value.arr.map(_.asInstanceOf[JDouble].num.longValue).toArray[Long])
      case (value: JArray, caseArrayDouble(v))  ⇒ Variable(v, value.arr.map(_.asInstanceOf[JDouble].num).toArray[Double])
      case (value: JArray, caseArrayString(v))  ⇒ Variable(v, value.arr.map(_.asInstanceOf[JString].s).toArray[String])
      case (value: JArray, caseArrayBoolean(v)) ⇒ Variable(v, value.arr.map(_.asInstanceOf[JBool].value).toArray[Boolean])

      case (jvalue, v)                          ⇒ throw new UserBadDataError(s"Impossible to store R output with value $jvalue in OpenMOLE variable $v.")
    }

  }

}

@Lenses case class RTask(
  script:             FromContext[String],
  uDocker:            UDockerArguments,
  errorOnReturnValue: Boolean,
  returnValue:        Option[Val[Int]],
  stdOut:             Option[Val[String]],
  stdErr:             Option[Val[String]],
  _config:            InputOutputConfig,
  external:           External,
  rInputs:            Vector[(Val[_], String)], rOutputs: Vector[(String, Val[_])]) extends Task with ValidateTask {

  override def config = UDockerTask.config(_config, returnValue, stdOut, stdErr)
  override def validate = container.validateContainer(Vector(), uDocker.environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    def writeInputsJSON(file: File) =
      file.content = compact(render(RTask.toJSONValue(rInputs.map { case (v, _) ⇒ context(v) }.toArray)))

    def rInputMapping(arrayName: String) =
      rInputs.zipWithIndex.map { case ((_, name), i) ⇒ s"$name = $arrayName[[${i + 1}]]" }.mkString("\n")

    def rOutputMapping =
      s"""c(${rOutputs.map { case (name, _) ⇒ name }.mkString(",")})"""

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
          |$inputArrayName = read_json("$inputJSONName")
          |${rInputMapping(inputArrayName)}
          |${script.from(p.context)(p.random, p.newFile, p.fileService)}
          |write_json($rOutputMapping, "$outputJSONName", always_decimal = TRUE)
          """.stripMargin

        val outputFile = Val[File]("outputFile", Namespace("RTask"))

        def uDockerTask = UDockerTask(uDocker, s"R --slave -f $rScriptName", errorOnReturnValue, returnValue, stdOut, stdErr, _config, external) set (
          resources += (scriptFile, rScriptName, true),
          resources += (jsonInputs, inputJSONName, true),
          outputFiles += (outputJSONName, outputFile),
          reuseContainer := true
        )

        val resultContext = uDockerTask.process(executionContext).from(context)
        resultContext ++ readOutputJSON(resultContext(outputFile))
      }
    }
  }
}