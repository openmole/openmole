package org.openmole.core.format

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.json4s.JArray
import org.openmole.core.context.Variable
import org.openmole.core.script.Imports.*
import org.openmole.core.format.*
import org.openmole.core.script.*
import org.openmole.core.json.*
import org.openmole.core.exception.*
import org.openmole.core.argument.*
import org.openmole.core.format.OutputFormat.*
import org.openmole.core.timeservice.TimeService
import org.openmole.tool.stream.{StringInputStream, inputStreamSequence}

import java.io.SequenceInputStream

object OMROutputFormat:
  implicit def outputFormat[M, MD](using default: OMROutputFormatDefault[M], methodData: MethodMetaData[M, MD], scriptData: ScriptSourceData): OutputFormat[OMROutputFormat, M] = new OutputFormat[OMROutputFormat, M]:
    override def write(executionContext: FormatExecutionContext)(f: OMROutputFormat, output: WritableOutput, content: OutputContent, method: M, append: Boolean): FromContext[Unit] = FromContext: p ⇒
      import p.*
      import org.json4s.*
      import executionContext.serializerService
      import executionContext.tmpDirectory
      import executionContext.timeService

      given Encoder[MD] = methodData.encoder
      val format = OMROutputFormatDefault.value(f, default)

      output match
        case WritableOutput.Display(_) ⇒
          implicitly[OutputFormat[CSVOutputFormat, Any]].write(executionContext)(CSVOutputFormat(), output, content, method).from(context)
        case WritableOutput.Store(file) ⇒
          def executionId = executionContext.moleExecutionId

          def methodFile =
            file.from(context) match
              case f if f.getName.endsWith(".omr") => f
              case f => f.getParentFile / s"${f.getName}.omr"

          def methodJson =
            methodData.data(method).asJson.mapObject(_.add(methodNameField, Json.fromString(methodData.name(method))))

          def script =
            scriptData match
              case data: ScriptSourceData.ScriptData if format.script ⇒
                val scriptContent = ScriptSourceData.scriptContent(scriptData)
                val imports =
                  val is = Imports.directImportedFiles(data.script).map(i ⇒ OMRContent.Import(ImportedFile.identifier(i), i.file.content))
                  if is.isEmpty then None else Some(is)

                Some(OMRContent.Script(scriptContent, imports))
              case _ ⇒ None


          OMRFormat.write(
            data = content,
            methodFile = methodFile,
            executionId = executionId,
            jobId = executionContext.jobId,
            methodJson = methodJson,
            script = script,
            timeStart = executionContext.moleLaunchTime,
            openMOLEVersion = org.openmole.core.buildinfo.version.value,
            append = append,
            overwrite = format.overwrite
          )

    override def validate(format: OMROutputFormat) = Validate.success



case class OMROutputFormat(
  script: OptionalArgument[Boolean] = None,
  overwrite: OptionalArgument[Boolean] = None,
  append: OptionalArgument[Boolean] = None)


object OMROutputFormatDefault:
  given default[T]: OMROutputFormatDefault[T] = OMROutputFormatDefault[T]()

  def value[T](format: OMROutputFormat, default: OMROutputFormatDefault[T]) =
    OMROutputFormatDefault[T](
      script = format.script.getOrElse(default.script),
      overwrite = format.overwrite.getOrElse(default.overwrite)
    )

case class OMROutputFormatDefault[T](
  script: Boolean = true,
  overwrite: Boolean = true)
