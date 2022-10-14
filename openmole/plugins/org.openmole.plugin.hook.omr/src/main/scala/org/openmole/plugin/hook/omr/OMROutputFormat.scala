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

  def methodFileName = "result.omr"

  def methodField = "method"
  def methodNameField = "type"

  def dataFileField = "data-file"
  def omrVersionField = "format-version"
  def omrVersion = "0.2"
  def scriptField = "script"
  def scriptContentField = "content"
  def scriptImportField = "import"
  def openMOLEVersionField = "openmole-version"

  def startTime = "start-time"
  def saveTime = "save-time"

  def contentData = "content-data"

  implicit def outputFormat[MD](implicit methodData: MethodData[MD], scriptData: ScriptSourceData): OutputFormat[OMROutputFormat, MD] = new OutputFormat[OMROutputFormat, MD] {
    override def write(executionContext: HookExecutionContext)(format: OMROutputFormat, output: WritableOutput, content: OutputContent, method: MD): FromContext[Unit] = FromContext { p ⇒
      import p.*
      import org.json4s.*
      import org.json4s.jackson.JsonMethods.*

      implicit val encoder = methodData.encoder

      output match {
        case WritableOutput.Display(_) ⇒
          implicitly[OutputFormat[CSVOutputFormat, Any]].write(executionContext)(CSVOutputFormat(), output, content, method).from(context)
        case WritableOutput.Store(file) ⇒
          def dataDirectory = "data"

          val directory = file.from(context)

          def methodFormat(method: MD, fileName: String, existingData: Seq[String], contentDataValue: ContentData) =
            import executionContext.timeService

            def updatedData = 
              val dataFiles = (existingData ++ Seq(fileName)).distinct
              Json.fromValues(dataFiles.map(Json.fromString))

            def methodJson = 
              method.asJson.mapObject(_.add(methodNameField, Json.fromString(methodData.name(method))))

            val o2 = Json.obj(
              methodField -> methodJson,
              dataFileField -> updatedData,
              omrVersionField -> Json.fromString(omrVersion),
              openMOLEVersionField -> Json.fromString(org.openmole.core.buildinfo.version.value),
              startTime -> Json.fromLong(executionContext.moleLaunchTime),
              saveTime -> Json.fromLong(TimeService.currentTime),
              contentData -> contentDataValue.asJson
            )
  
            scriptData match
              case data: ScriptSourceData.ScriptData if format.script ⇒
                case class OMRImport(`import`: String, content: String)

                val scriptContent = ScriptSourceData.scriptContent(scriptData)
                val imports = Imports.directImportedFiles(data.script)

                o2.mapObject {
                  _.add(
                    scriptField,
                    Json.obj (
                      scriptContentField -> Json.fromString(scriptContent),
                      scriptImportField -> Json.fromValues(imports.map(i ⇒ OMRImport(ImportedFile.identifier(i), i.file.content).asJson))
                    )
                  )
                 }
              case _ ⇒ o2

          def parseExistingData(file: File): Seq[String] = 
            import org.json4s.DefaultReaders.*
            try 
              if file.exists 
              then
                val j = parse(file.content(gz = true)) 
                (j \ dataFileField).getAs[Seq[String]].get
              else Seq()
            catch 
             case e: Throwable => throw new InternalProcessingError(s"Error parsing existing method file ${file}", e)
            
          directory.withLockInDirectory {
            val methodFile = directory / methodFileName
            val existingData = if methodFile.exists then parseExistingData(methodFile) else Seq()

            content match
              case PlainContent(name, variables) ⇒
                val fileName = s"$dataDirectory/${name}.json.gz"
                val dataFile = directory / fileName

                dataFile.withPrintStream(append = false, create = true, gz = true) { ps ⇒
                  ps.print(compact(render(variablesToJValue(variables))))
                }

                def content = ContentData.Plain(variables.map(v => ValData(v.prototype)))

                methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method, fileName, existingData, content).noSpaces))
              case sections: SectionContent ⇒
                val fileName = s"$dataDirectory/${sections.name}.json.gz"
                val dataFile = directory / fileName

                val content =
                  JObject(
                    sections.content.map { section ⇒ section.name -> variablesToJValue(section.variables) }.toList
                  )

                dataFile.withPrintStream(append = false, create = true, gz = true) { ps ⇒
                  ps.print(compact(render(content)))
                }

                def contentData = ContentData.Section(sections.content.map(s => ContentData.SectionData(s.name, s.variables.map(v => ValData(v.prototype)))))

                methodFile.withPrintStream(create = true, gz = true)(_.print(methodFormat(method, fileName, existingData, contentData).noSpaces))
          }
      }
    }

    override def validate(format: OMROutputFormat) = Validate.success
  }

  case class OMRData(method: String, fileName: String, version: String)

  def omrData(file: File): OMRData = {
    import io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
    val content = file.content(gz = true)
    val parseResult = parse(content)
    parseResult match {
      case Right(r) ⇒
        def readField(f: String) =
          r.hcursor.downField(f).as[String] match {
            case Right(r) ⇒ r
            case Left(e)  ⇒ throw new InternalProcessingError(s"Unable to get field $f in json:\n$content", e)
          }
        OMRData(
          method = readField(methodNameField),
          fileName = readField(dataFileField),
          version = readField(omrVersionField)
        )
      case Left(e) ⇒ throw new InternalProcessingError(s"Error while parsing omr file $file with content:\n$content", e)
    }

  }

}

case class OMROutputFormat(script: Boolean = true)
