package org.openmole.core.format

/*
 * Copyright (C) 2023 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.json4s.JArray
import org.json4s.JsonAST.{JField, JString}
import org.json4s.jackson.JsonMethods.{compact, render}
import org.openmole.core.json.*
import org.openmole.core.context.{ValType, Variable}
import org.openmole.core.exception.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.format.OutputFormat.SectionContent
import org.openmole.core.serializer.SerializerService
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.stream.{StringInputStream, inputStreamSequence}
import org.openmole.tool.file.*

import java.util.UUID

implicit val omrCirceDefault: io.circe.derivation.Configuration =
 io.circe.derivation.Configuration.default.withKebabCaseMemberNames.withDefaults.withDiscriminator("type").withTransformConstructorNames(derivation.renaming.kebabCase)

object OMRContent:
 case class Import(`import`: String, content: String) derives derivation.ConfiguredCodec
 case class Script(content: String, `import`: Option[Seq[Import]]) derives derivation.ConfiguredCodec

 object DataMode:
  given Encoder[DataMode] = Encoder.instance:
   case DataMode.Append => Encoder.encodeString("append")
   case DataMode.Create => Encoder.encodeString("create")

  given Decoder[DataMode] =
    Decoder.decodeString.map:
      case "append" => DataMode.Append
      case "create" => DataMode.Create

 enum DataMode:
  case Append, Create

 object Compression:
  given Encoder[Compression] = Encoder.instance:
   case Compression.GZip => Encoder.encodeString("gzip")

  given Decoder[Compression] = Decoder.decodeString.map:
   case "gzip" => Compression.GZip

 enum Compression:
  case GZip

 object DataContent:
   case class SectionData(name: Option[String], variables: Seq[ValData], indexes: Option[Seq[String]] = None)derives derivation.ConfiguredCodec

 case class DataContent(section: Seq[DataContent.SectionData])derives derivation.ConfiguredCodec

case class OMRContent(
  `format-version`: String,
  `openmole-version`: String,
  `execution-id`: String,
  `data-file`: Seq[String],
  `data-mode`: OMRContent.DataMode,
  `data-content`: OMRContent.DataContent,
  `data-compression`: Option[OMRContent.Compression] = None,
  `file-directory`: Option[String],
  script: Option[OMRContent.Script],
  `time-start`: Long,
  `time-save`: Long,
  method: Option[Json]) derives derivation.ConfiguredCodec

def methodNameField = "method-name"
def omrVersion = "1.0"
def dataDirectoryName = ".omr-data"

object OMRFormat:
  def resultFileDirectoryName(executionId: String) =
    s"$dataDirectoryName/files-${executionId.filter(_ != '-')}"

  def resultFileDirectory(file: File) =
    val index = omrContent(file)
    index.`file-directory`.map(d => file.getParentFile / d)

  def dataDirectory(file: File) =
    file.getParentFile / dataDirectoryName

  def isOMR(file: File) = file.getName.endsWith(".omr")

  def omrContent(file: File): OMRContent =
    val content = file.content(gz = true)
    decode[OMRContent](content).toTry.get

  def dataFiles(file: File): Seq[(String, File)] =
    val directory = file.getParentFile
    omrContent(file).`data-file`.map { f => (f, directory / f) }

  def writeOMRContent(file: File, content: OMRContent) =
    file.withPrintStream(create = true, gz = true)(
      _.print(content.asJson.deepDropNullValues.noSpaces)
    )

  def write(
    data: OutputFormat.OutputContent,
    methodFile: File,
    executionId: String,
    jobId: Long,
    methodJson: Json,
    script: Option[OMRContent.Script],
    timeStart: Long,
    openMOLEVersion: String,
    append: Boolean,
    overwrite: Boolean)(using TimeService, FileService, TmpDirectory, SerializerService) =
    val resultFileDirectoryName = OMRFormat.resultFileDirectoryName(executionId)

    def methodFormat(existingData: Seq[String], fileName: String, dataContent: OMRContent.DataContent, fileDirectory: Option[String]) =
      def mode =
        if append
        then OMRContent.DataMode.Append
        else OMRContent.DataMode.Create

      OMRContent(
        `format-version` = omrVersion,
        `openmole-version` = openMOLEVersion,
        `execution-id` = executionId,
        `data-file` = (existingData ++ Seq(fileName)).distinct,
        `data-mode` = mode,
        `data-content` = dataContent,
        `file-directory` = fileDirectory,
        script = script,
        `time-start` = timeStart,
        `time-save` = TimeService.currentTime,
        method = Some(methodJson)
      )


    def parseExistingData(file: File): Option[(String, Seq[String])] =
      try
        if file.exists
        then
          val data = OMRFormat.omrContent(file)
          Some((data.`execution-id`, data.`data-file`))
        else None
      catch
        case e: Throwable => throw new InternalProcessingError(s"Error parsing existing method file ${file}", e)

    val directory = methodFile.getParentFile

    directory.withLockInDirectory:
      val existingData =
        parseExistingData(methodFile) match
          case Some((id, data)) if overwrite && id != executionId =>
            OMRFormat.delete(methodFile) //clean(methodFile, data)
            Seq()
          case Some((_, data)) => data
          case None => Seq()

      val storeFileDirectory = directory / resultFileDirectoryName

      def storeFile(f: File) =
        val destinationPath = s"${summon[FileService].hashNoCache(f)}/${f.getName}"
        f.copy(storeFileDirectory / destinationPath)
        org.json4s.JString(destinationPath)

      def jsonContent = JArray(data.section.map { s => JArray(variablesToJValues(s.variables, default = Some(anyToJValue), file = Some(storeFile)).toList) }.toList)

      val fileName =
        def executionPrefix = executionId.filter(_ != '-')
        def newUUID = UUID.randomUUID().toString.filter(_ != '-')
        if !append
        then s"$dataDirectoryName/$executionPrefix-$newUUID.omd"
        else
          existingData.headOption match
            case Some(h) => h
            case None => s"$dataDirectoryName/$executionPrefix-$newUUID.omd"

      val dataFile = directory / fileName

      dataFile.withPrintStream(append = append, create = true, gz = true): ps ⇒
        if append && existingData.nonEmpty then ps.print(",\n")
        ps.print(compact(render(jsonContent)))

      def contentData =
        OMRContent.DataContent:
          data.section.map: s =>
            def sectionIndex = if s.indexes.nonEmpty then Some(s.indexes) else None
            OMRContent.DataContent.SectionData(s.name, s.variables.map(v => ValData(v.prototype)), sectionIndex)

      // Is created by variablesToJValues if it found some files
      def fileDirectoryValue =
        if storeFileDirectory.exists()
        then Some(resultFileDirectoryName)
        else None

      writeOMRContent(
        methodFile,
        methodFormat(existingData, fileName, contentData, fileDirectoryValue)
      )

//  def resultFiles(file: File): Seq[(String, File)] =
//    val directory = file.getParentFile
//    val files = collection.mutable.ListBuffer[(String, File)]()
//    def listFilePath(v: org.json4s.JValue) =
//      val path = OMR.loadFilePath(directory, v)
//      val file = directory / path
//      files += (path, file)
//      f
//
//    toVariables(file, loadFilePath = listFilePath)
//    files.toSeq

  def copy(omrFile: File, destination: File) =
    val originDirectory = omrFile.getParentFile
    val destinationDirectory = destination.getParentFile
    val copyData = originDirectory != destinationDirectory
    if copyData
    then
      dataFiles(omrFile).foreach((name, f) => f.copy(destinationDirectory / name))
      val index = omrContent(omrFile)
      index.`file-directory`.foreach(d => (originDirectory / d).copy(destinationDirectory / d))

    omrFile copy destination

  def move(omrFile: File, destination: File) =
    val originDirectory = omrFile.getParentFile
    val destinationDirectory = destination.getParentFile
    val moveData = originDirectory != destinationDirectory
    if moveData
    then
      val destinationDataDirectory = destination.getParentFile
      val index = omrContent(omrFile)
      index.`file-directory`.foreach(d => (originDirectory / d).move(destinationDirectory / d))
      dataFiles(omrFile).foreach((name, f) => f.move(destinationDataDirectory / name))
      val omrDataDirectory = dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    omrFile move destination

  def delete(omrFile: File) =
    dataFiles(omrFile).foreach((_, f) => f.delete())
    resultFileDirectory(omrFile).foreach(_.recursiveDelete)
    val omrDataDirectory = dataDirectory(omrFile)
    if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    omrFile.delete()

  def diskUsage(omrFile: File) =
    omrFile.size +
      OMRFormat.dataFiles(omrFile).map(_._2.size).sum +
      OMRFormat.resultFileDirectory(omrFile).map(_.size).getOrElse(0L)

  def pruneHistory(omrFile: File) =
    val df = dataFiles(omrFile)
    val keep = df.last
    val content = omrContent(omrFile)
    val newContent = content.copy(`data-file` = Seq(keep._1))
    try writeOMRContent(omrFile, newContent)
    finally df.dropRight(1).foreach((_, f) => f.delete())
  
  def variables(
    file: File,
    relativePath: Boolean = false,
    dataFile: Option[String] = None)(using serializerService: SerializerService): Seq[(OMRContent.DataContent.SectionData, Seq[Variable[_]])] =
    val index = omrContent(file)
    val omrDirectory = file.getParentFile
    val data: File = omrDirectory / (dataFile getOrElse index.`data-file`.last)

    def loadFile(v: org.json4s.JValue) =
      import org.openmole.core.json.*
      v match
        case jv: org.json4s.JString =>
          index.`file-directory` match
            case Some(fileDirectory) if !relativePath => omrDirectory / fileDirectory / jv.s
            case _ => File(jv.s)
        case _ => cannotConvertFromJSON[File](v)

    index.`data-mode` match
      case OMRContent.DataMode.Create =>
        def sectionToVariables(section: OMRContent.DataContent.SectionData, a: JArray) =
          section -> (section.variables zip a.arr).map { (v, j) => jValueToVariable(j, ValData.toVal(v), file = Some(loadFile), default = Some(jValueToAny)) }

        def readContent(file: File): JArray =
          file.withGzippedInputStream { is =>
            import org.json4s.jackson.JsonMethods.*
            parse(is).asInstanceOf[JArray]
          }

        val content = readContent(data)
        (index.`data-content`.section zip content.arr).map((s, c) => sectionToVariables(s, c.asInstanceOf[JArray]))
      case OMRContent.DataMode.Append =>
        def sectionToAggregatedVariables(section: OMRContent.DataContent.SectionData, sectionIndex: Int, content: JArray) =
          val size = section.variables.size
          val sectionContent = content.arr.map(a => a.asInstanceOf[JArray].arr(sectionIndex))
          def transposed = (0 until size).map { i => JArray(sectionContent.map(_.asInstanceOf[JArray](i))) }
          section -> (section.variables zip transposed).map { (v, j) => jValueToVariable(j, ValData.toVal(v).toArray, file = Some(loadFile), default = Some(jValueToAny)) }

        def readContent(file: File): JArray =
          val begin = new StringInputStream("[")
          val end = new StringInputStream("]")
          file.withGzippedInputStream { is =>
            val s = inputStreamSequence(begin, is, end)
            import org.json4s.jackson.JsonMethods.*
            parse(s).asInstanceOf[JArray]
          }

        val content = readContent(data)
        index.`data-content`.section.zipWithIndex.map((s, i) => sectionToAggregatedVariables(s, i, content))


  object IndexedData:
    type FileIndex = String

  case class IndexedData(fileIndex: IndexedData.FileIndex, sectionIndex: Int, variable: Variable[_])

  def indexes(file: File)(using SerializerService): Seq[IndexedData] =
    val content = omrContent(file)
    dataFiles(file).flatMap: (f, _) =>
      val sectionVariables = variables(file, dataFile = Some(f))
      sectionVariables.zipWithIndex.flatMap:
        case ((section, variables), i) =>
          val names = section.indexes.getOrElse(Seq()).toSet
          variables.filter(v => names.contains(v.name)).map: v =>
            IndexedData(f, i, v)

  def variablesAtIndex(file: File, index: IndexedData.FileIndex)(using SerializerService) =
    variables(file, dataFile = Some(index))

  def methodName(file: File): Option[String] =
    val content = omrContent(file)
    content.method.flatMap: j =>
      j.hcursor.downField(methodNameField).as[String].toOption

  def writeCSV(
    file: File,
    destination: File,
    dataFile: Option[String] = None,
    unrollArray: Boolean = true,
    arrayOnRow: Boolean = false,
    gzip: Boolean = false)(using SerializerService) =
    val variable = variables(file, relativePath = true, dataFile = dataFile)

    if variable.size == 1
    then
      CSVFormat.writeVariablesToCSV(
        destination,
        variable.head._2,
        unrollArray = unrollArray,
        arrayOnRow = arrayOnRow,
        gzip = gzip)
    else
      destination.clear
      for
        ((section, v), i) <- variable.zipWithIndex
      do
        destination.append(s"#section: ${section.name.getOrElse(i.toString)}\n")
        CSVFormat.writeVariablesToCSV(
          destination,
          v,
          unrollArray = unrollArray,
          arrayOnRow = arrayOnRow,
          gzip = gzip,
          append = true
        )

  def writeJSON(
    file: File,
    destination: File,
    dataFile: Option[String] = None)(using SerializerService) =

    val index = omrContent(file)
    def variablesValues = variables(file, relativePath = true, dataFile = dataFile)

    case class JSONContent(
      `openmole-version`: String,
      `execution-id`: String,
      script: Option[OMRContent.Script],
      `time-start`: Long,
      `time-save`: Long) derives derivation.ConfiguredCodec

    import OMRContent.given

    def jsonData =
      org.json4s.JArray(
        variablesValues.map: (s, variables) =>
          def content: Seq[(String, org.json4s.JValue)] =
            def fileToJSON(f: File) = JString(f.getPath)
            s.name.map(n => "name" -> org.json4s.JString(n)).toSeq ++
              Seq("variables" -> variablesToJObject(variables, default = Some(anyToJValue), file = Some(fileToJSON)))
          org.json4s.JObject(content.toList)
        .toList
      )

    def jsonContent =
      JSONContent(
        `openmole-version` = index.`openmole-version`,
        `execution-id` = index.`execution-id`,
        script = index.script,
        `time-start` = index.`time-start`,
        `time-save` = index.`time-save`
      )

    val renderedContent = org.json4s.jackson.parseJson(jsonContent.asJson.deepDropNullValues.noSpaces).asInstanceOf[org.json4s.JObject]
    destination.content =
      import org.json4s.jackson
      val fullObject = renderedContent.copy(obj = renderedContent.obj ++ Seq("data" -> jsonData))
      jackson.prettyJson(jackson.renderJValue(fullObject))

export org.openmole.core.format.{CSVOutputFormat as CSV, OMROutputFormat as OMR}
def defaultOutputFormat = OMR()