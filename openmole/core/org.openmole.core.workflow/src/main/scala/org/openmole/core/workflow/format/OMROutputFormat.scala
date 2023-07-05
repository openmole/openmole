package org.openmole.core.workflow.format

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.json4s.JArray
import org.openmole.core.context.Variable
import org.openmole.core.script.Imports.*
import org.openmole.core.omr.*
import org.openmole.core.workflow.hook.*
import org.openmole.core.workflow.format.*
import org.openmole.core.workflow.tools.*
import org.openmole.core.script.*
import org.openmole.core.json.*
import org.openmole.core.exception.*
import org.openmole.core.expansion.*
import org.openmole.core.workflow.format.OutputFormat.*
import org.openmole.core.timeservice.TimeService
import org.openmole.tool.stream.{StringInputStream, inputStreamSequence}

import java.io.SequenceInputStream

object OMROutputFormat:

  implicit def outputFormat[M, MD](using default: OMROutputFormatDefault[M], methodData: MethodMetaData[M, MD], scriptData: ScriptSourceData): OutputFormat[OMROutputFormat, M] = new OutputFormat[OMROutputFormat, M]:
    override def write(executionContext: HookExecutionContext)(f: OMROutputFormat, output: WritableOutput, content: OutputContent, method: M): FromContext[Unit] = FromContext: p ⇒
      import p.*
      import org.json4s.*
      import org.json4s.jackson.JsonMethods.*
      import executionContext.serializerService

      given Encoder[MD] = methodData.encoder
      val format = OMROutputFormatDefault.value(f, default)

      output match
        case WritableOutput.Display(_) ⇒
          implicitly[OutputFormat[CSVOutputFormat, Any]].write(executionContext)(CSVOutputFormat(), output, content, method).from(context)
        case WritableOutput.Store(file) ⇒
          def executionId = executionContext.moleExecutionId

          val (methodFile, directory) =
            file.from(context) match
              case f if f.getName.endsWith(".omr") => (f, f.getParentFile)
              case f => (f.getParentFile / s"${f.getName}.omr", f.getParentFile)

          def methodFormat(method: M, fileName: String, existingData: Seq[String], dataContentValue: DataContent) =
            import executionContext.timeService

            def methodJson =
              methodData.data(method).asJson.mapObject(_.add(methodPluginField, Json.fromString(methodData.plugin(method))))
                //.asObject.get.toList.head._2.mapObject(_.add(methodNameField, Json.fromString(methodData.name(method))))

            val script =
              scriptData match
                case data: ScriptSourceData.ScriptData if format.script ⇒
                  val scriptContent = ScriptSourceData.scriptContent(scriptData)
                  val imports =
                    val is = Imports.directImportedFiles(data.script).map(i ⇒ Index.Import(ImportedFile.identifier(i), i.file.content))
                    if is.isEmpty then None else Some(is)

                  Some(Index.Script(scriptContent, imports))
                case _ ⇒ None

            def mode =
              if format.append
              then Index.DataMode.Append
              else Index.DataMode.Create

            val result =
              Index(
                `format-version` = omrVersion,
                `openmole-version` = org.openmole.core.buildinfo.version.value,
                `execution-id` = executionId,
                 `data-file` = (existingData ++ Seq(fileName)).distinct,
                `data-mode` = mode,
                `data-content` = dataContentValue,
                `data-compression` = Some(Index.Compression.GZip),
                script = script,
                `time-start` = executionContext.moleLaunchTime,
                `time-save` = TimeService.currentTime
              )

            result.asJson.
              mapObject(_.add(methodField, methodJson)).
              asJson.
              deepDropNullValues

          def parseExistingData(file: File): Option[(String, Seq[String])] =
            try
              if file.exists
              then
                val data = OMR.indexData(file)
                Some((data.`execution-id`, data.`data-file`))
              else None
            catch
             case e: Throwable => throw new InternalProcessingError(s"Error parsing existing method file ${file}", e)

          def clean(methodFile: File, data: Seq[String]) =
            methodFile.delete()
            for d <- data do (directory / d).delete()

          directory.withLockInDirectory:
            val existingData =
              parseExistingData(methodFile) match
                case Some((id, data)) if format.overwrite && id != executionContext.moleExecutionId =>
                  clean(methodFile, data)
                  Seq()
                case Some((_, data)) => data
                case None => Seq()

            def executionPrefix = executionId.filter(_ != '-')

            val fileName =
              if !format.append
              then s"$dataDirectory/$executionPrefix-${executionContext.jobId}.omd"
              else s"$dataDirectory/$executionPrefix.omd"

            val dataFile = directory / fileName

            def jsonContent = JArray(content.section.map { s => JArray(variablesToJValues(s.variables, default = Some(anyToJValue)).toList) }.toList)

            dataFile.withPrintStream(append = format.append, create = true, gz = true) { ps ⇒
              if format.append && existingData.nonEmpty then ps.print(",\n")
              ps.print(compact(render(jsonContent)))
            }

            def contentData = DataContent(content.section.map { s => DataContent.SectionData(s.name, s.variables.map(v => ValData(v.prototype))) })

            methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method, fileName, existingData, contentData).noSpaces))

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
      overwrite = format.overwrite.getOrElse(default.overwrite),
      append = format.append.getOrElse(default.append)
    )

case class OMROutputFormatDefault[T](
  script: Boolean = true,
  overwrite: Boolean = true,
  append: Boolean = false)
