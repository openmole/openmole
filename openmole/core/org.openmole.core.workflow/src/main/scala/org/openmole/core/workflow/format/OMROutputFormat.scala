package org.openmole.core.workflow.format

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.json4s.JArray
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
  def methodFileName = "index.omr"

  object Index:
    given Codec[Import] = Codec.AsObject.derivedConfigured
    given Codec[Script] = Codec.AsObject.derivedConfigured
    given Codec[Index] = Codec.AsObject.derivedConfigured

    case class Import(`import`: String, content: String)
    case class Script(content: String, `import`: Option[Seq[Import]])

    enum DataMode:
      case Append, Create

  case class Index(
    `format-version`: String,
    `openmole-version`: String,
    `execution-id`: String,
    `data-file`: Seq[String],
    `data-mode`: Index.DataMode,
    `data-content`: DataContent,
    script: Option[Index.Script],
    `time-start`: Long,
    `time-save`: Long)

  def methodField = "method"
  def methodPluginField = "plugin"
  def omrVersion = "0.2"

  implicit def outputFormat[MD](using default: OMROutputFormatDefault[MD], methodData: MethodMetaData[MD], scriptData: ScriptSourceData): OutputFormat[OMROutputFormat, MD] = new OutputFormat[OMROutputFormat, MD]:
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

          def methodFormat(method: MD, fileName: String, existingData: Seq[String], dataContentValue: DataContent) =
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
                `data-content` = dataContentValue,
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
              else s"data/data.omd"

            val dataFile = directory / fileName

            def jsonContent = JArray(content.section.map { s => JArray(variablesToJValues(s.variables, default = Some(anyToJValue)).toList) }.toList)

            dataFile.withPrintStream(append = format.append, create = true, gz = true) { ps ⇒
              if format.append && existingData.nonEmpty then ps.print(",\n")
              ps.print(compact(render(jsonContent)))
            }

            def contentData = DataContent(content.section.map { s => DataContent.SectionData(s.name, s.variables.map(v => ValData(v.prototype))) })

            methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method, fileName, existingData, contentData).noSpaces))
          }
      }
    }

    override def validate(format: OMROutputFormat) = Validate.success


  def indexData(file: File): Index =
    val content = file.content(gz = true)
    decode[Index](content).toTry.get

  def toVariables(file: File) =
    val index = indexData(file)
    val data: File = file.getParentFile / index.`data-file`.last

    index.`data-mode` match
      case OMROutputFormat.Index.DataMode.Create =>
        def sectionToVariables(section: DataContent.SectionData, a: JArray) =
          (section.variables zip a.arr).map { (v, j) => jValueToVariable(j, ValData.toVal(v)) }

        def readContent(file: File): JArray =
          file.withGzippedInputStream { is =>
            import org.json4s.jackson.JsonMethods.*
            parse(is).asInstanceOf[JArray]
          }

        val content = readContent(data)
        (index.`data-content`.section zip content.arr).map((s, c) => sectionToVariables(s, c.asInstanceOf[JArray]))
      case OMROutputFormat.Index.DataMode.Append =>
        def sectionToAggregatedVariables(section: DataContent.SectionData, sectionIndex: Int, content: JArray) =
          val size = section.variables.size
          val sectionContent = content.arr.map(a => a.asInstanceOf[JArray].arr(sectionIndex))
          def transposed = (0 until size).map { i => JArray(sectionContent.map(_.asInstanceOf[JArray](i))) }
          (section.variables zip transposed).map { (v, j) => jValueToVariable(j, ValData.toVal(v).toArray) }

        def readContent(file: File): JArray =
          val begin = new StringInputStream("[")
          val end = new StringInputStream("]")
          file.withGzippedInputStream { is =>
            val s = inputStreamSequence(begin, is, end)
            import org.json4s.jackson.JsonMethods.*
            parse(s).asInstanceOf[JArray]
          }

        val content = readContent(data)
        (index.`data-content`.section zipWithIndex).map((s, i) => sectionToAggregatedVariables(s, i, content))

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
