package org.openmole.core.omr

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.openmole.core.script.Imports.*
import org.openmole.core.workflow.hook.*
import org.openmole.core.workflow.format.*
import org.openmole.core.workflow.tools.*
import org.openmole.core.script.*
import org.openmole.core.json.*
import org.openmole.core.exception.*
import org.openmole.core.expansion.*
import org.openmole.core.workflow.format.OutputFormat.*
import org.openmole.core.omr.data.*
import org.openmole.core.timeservice.TimeService
import org.openmole.tool.file.*


object OMROutputFormat:
  def methodFileName = "index.omr"

  object Index:
    given Codec[Import] = Codec.AsObject.derivedConfigured
    given Codec[Script] = Codec.AsObject.derivedConfigured
    given Codec[Time] = Codec.AsObject.derivedConfigured
    given Codec[Index] = Codec.AsObject.derivedConfigured

    case class Import(`import`: String, content: String)
    case class Script(content: String, `import`: Option[Seq[Import]])
    case class Time(start: Long, save: Long)

    enum DataMode:
      case Append, Create

  case class Index(
    `format-version`: String,
    `openmole-version`: String,
    `execution-id`: String,
    `data-file`: Seq[String],
    `data-mode`: Index.DataMode,
    script: Option[Index.Script],
    `content-data`: ContentData,
    time: Index.Time)

  def methodField = "method"
  def methodPluginField = "plugin"
  def omrVersion = "0.2"

  implicit def outputFormat[MD](using default: OMROutputFormatDefault[MD], methodData: MethodMetaData[MD], scriptData: ScriptSourceData): OutputFormat[OMROutputFormat, MD] = new OutputFormat[OMROutputFormat, MD] {
    override def write(executionContext: HookExecutionContext)(f: OMROutputFormat, output: WritableOutput, content: OutputContent, method: MD): FromContext[Unit] = FromContext { p ⇒
      import p.*
      import org.json4s.*
      import org.json4s.jackson.JsonMethods.*
      import executionContext.serializerService

      implicit val encoder = methodData.encoder
      val format = OMROutputFormatDefault.value(f, default)

      output match {
        case WritableOutput.Display(_) ⇒
          implicitly[OutputFormat[CSVOutputFormat, Any]].write(executionContext)(CSVOutputFormat(), output, content, method).from(context)
        case WritableOutput.Store(file) ⇒
          val directory = file.from(context)

          def methodFormat(method: MD, fileName: String, existingData: Seq[String], contentDataValue: ContentData) =
            import executionContext.timeService

            def methodJson =
              method.asJson.mapObject(_.add(methodPluginField, Json.fromString(methodData.plugin(method))))
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
                `execution-id` = executionContext.moleExecutionId,
                 `data-file` = (existingData ++ Seq(fileName)).distinct,
                `data-mode` = mode,
                script = script,
                `content-data` = contentDataValue,
                time = Index.Time(
                  start = executionContext.moleLaunchTime,
                  save = TimeService.currentTime
                )
              )

            result.asJson.
              mapObject(_.add(methodField, methodJson)).
              asJson.
              deepDropNullValues

          def parseExistingData(file: File): Option[(String, Seq[String])] =
            try
              if file.exists
              then
                val data = indexData(file)
                Some((data.`execution-id`, data.`data-file`))
              else None
            catch
             case e: Throwable => throw new InternalProcessingError(s"Error parsing existing method file ${file}", e)

          def clean(methodFile: File, data: Seq[String]) =
            methodFile.delete()
            for d <- data do (directory / d).delete()

          directory.withLockInDirectory {
            val methodFile = directory / methodFileName

            val existingData =
              parseExistingData(methodFile) match
                case Some((id, data)) if format.overwrite && id != executionContext.moleExecutionId =>
                  clean(methodFile, data)
                  Seq()
                case Some((_, data)) => data
                case None => Seq()

            val fileName =
              if !format.append
              then s"data/${executionContext.jobId}.omd"
              else s"data.omd"

            val dataFile = directory / fileName

            def jsonContent = JArray(content.section.map { s => JArray(variablesToJValues(s.variables, default = Some(anyToJValue)).toList) }.toList)

            dataFile.withPrintStream(append = format.append, create = true, gz = true) { ps ⇒
              if format.append && existingData.nonEmpty then ps.print(",\n")
              ps.print(compact(render(jsonContent)))
            }

            def contentData = ContentData(content.section.map { s => ContentData.SectionData(s.name, s.variables.map(v => ValData(v.prototype))) })

            methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method, fileName, existingData, contentData).noSpaces))
          }
      }
    }

    override def validate(format: OMROutputFormat) = Validate.success
  }

  def indexData(file: File): Index =
    val content = file.content(gz = true)
    decode[Index](content).toTry.get


  def methodName(file: File): String =
    val j = parse(file.content(gz = true)).toTry.get
    j.hcursor.
      downField(methodField).
      downField( methodPluginField).as[String].
      toTry.get


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
