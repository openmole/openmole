package org.openmole.plugin.hook.omr

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.format.CSVOutputFormat
import org.openmole.core.project._
import org.openmole.plugin.tool.json._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.format.OutputFormat.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*

import org.openmole.core.project.Imports.ImportedFile
import org.openmole.plugin.tool.methoddata.*

object OMROutputFormat {

  def methodFileName = "index.omr"

  object Index:
    case class Import(`import`: String, content: String)
    case class Script(content: String, `import`: Option[Seq[Import]])
    case class Time(start: Long, save: Long)

  case class Index(
    `format-version`: String,
    `openmole-version`: String,
    `data-file`: Seq[String],
    script: Option[Index.Script],
    `content-data`: ContentData,
    time: Index.Time)

  def methodField = "method"
  def methodNameField = "name"
  def omrVersion = "0.2"

  implicit def outputFormat[MD](implicit methodData: MethodData[MD], scriptData: ScriptSourceData): OutputFormat[OMROutputFormat, MD] = new OutputFormat[OMROutputFormat, MD] {
    override def write(executionContext: HookExecutionContext)(format: OMROutputFormat, output: WritableOutput, content: OutputContent, method: MD): FromContext[Unit] = FromContext { p ⇒
      import p.*
      import org.json4s.*
      import org.json4s.jackson.JsonMethods.*
      import executionContext.serializerService

      implicit val encoder = methodData.encoder

      output match {
        case WritableOutput.Display(_) ⇒
          implicitly[OutputFormat[CSVOutputFormat, Any]].write(executionContext)(CSVOutputFormat(), output, content, method).from(context)
        case WritableOutput.Store(file) ⇒
          def dataDirectory = "data"

          val directory = file.from(context)

          def methodFormat(method: MD, fileName: String, existingData: Seq[String], contentDataValue: ContentData) =
            import executionContext.timeService

            def methodJson = 
              method.asJson.asObject.get.toList.head._2.
              mapObject(_.add(methodNameField, Json.fromString(methodData.name(method))))

            val script =
              scriptData match
                case data: ScriptSourceData.ScriptData if format.script ⇒
                  val scriptContent = ScriptSourceData.scriptContent(scriptData)
                  val imports =
                    val is = Imports.directImportedFiles(data.script).map(i ⇒ Index.Import(ImportedFile.identifier(i), i.file.content))
                    if is.isEmpty then None else Some(is)

                  Some(Index.Script(scriptContent, imports))
                case _ ⇒ None

            val result =
              Index(
                `format-version` = omrVersion,
                `openmole-version` = org.openmole.core.buildinfo.version.value,
                `data-file` = (existingData ++ Seq(fileName)).distinct,
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

          def parseExistingData(file: File): Seq[String] =
            try 
              if file.exists 
              then indexData(file).`data-file`
              else Seq()
            catch 
             case e: Throwable => throw new InternalProcessingError(s"Error parsing existing method file ${file}", e)
            
          directory.withLockInDirectory {
            val methodFile = directory / methodFileName
            val existingData = if methodFile.exists then parseExistingData(methodFile) else Seq()
            val fileName = s"$dataDirectory/${executionContext.jobId}.json.gz"

            val dataFile = directory / fileName

            def jsonContent = JArray(content.section.map { s => variablesToJValue(s.variables, default = Some(anyToJValue)) }.toList)

            dataFile.withPrintStream(append = false, create = true, gz = true) { ps ⇒
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
      downField( methodNameField).as[String].
      toTry.get

}

case class OMROutputFormat(script: Boolean = true)
