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


import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.json4s.{JArray, JValue}
import org.json4s.JsonAST.{JField, JString}
import org.json4s.jackson.JsonMethods.{compact, render}
import org.openmole.core.json.*
import org.openmole.core.context.{Val, ValType, Variable}
import org.openmole.core.exception.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.format.OMRFormatV2.APIV2.SectionContent
import org.openmole.core.format.OMRFormatV2.DataMode
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.stream.{DefaultBufferSize, StringInputStream, inputStreamSequence}
import org.openmole.tool.file.*
import org.openmole.tool.archive.*

import java.util
import java.util.UUID
import scala.collection.mutable

object OMRFormat:
  def methodNameField = "method-name"
  def dataDirectoryName = ".omr-data"
  def formatVersionField = "format-version"

  enum Version:
    case V1, V2

  case class Import(`import`: String, content: String) derives derivation.ConfiguredCodec
  case class Script(content: String, `import`: Option[Seq[Import]]) derives derivation.ConfiguredCodec

  object API:
    case class SectionData(name: Option[String], variables: Seq[ValData]) derives derivation.ConfiguredCodec

  object Embedable:
    extension (e: Embedable)
      def omrFile =
        e match
          case e: OMRFormatV1.EmbedableContent => e.omrFile
          case e: OMRFormatV2.EmbedableContent => e.omrFile

  sealed trait Embedable


  extension (f: OMRFormat)
    def openMOLEVersion =
      f match
        case f: OMRFormatV1 => f.`openmole-version`
        case f: OMRFormatV2 => f.`openmole-version`

    def executionID =
      f match
        case f: OMRFormatV1 => f.`execution-id`
        case f: OMRFormatV2 => f.`execution-id`

    def script =
      f match
        case f: OMRFormatV1 => f.script
        case f: OMRFormatV2 => f.script

    def timeStart =
      f match
        case f: OMRFormatV1 => f.`time-start`
        case f: OMRFormatV2 => f.`time-start`

    def timeSave =
      f match
        case f: OMRFormatV1 => f.`time-save`
        case f: OMRFormatV2 => f.`time-save`

    def method =
      f match
        case f: OMRFormatV1 => f.method
        case f: OMRFormatV2 => f.method

  def version(omrFile: File): Version =
    readSingleJSONField(omrFile, formatVersionField, gz = true).map(_.takeWhile(_.isDigit).toInt) match
      case Some(1) => Version.V1
      case Some(2) => Version.V2
      case Some(v) => throw UserBadDataError(s"Unsupported format version: $v")
      case None => throw UserBadDataError(s"Format version field not found in $omrFile")

  object omr:
    def newUUID = UUID.randomUUID().toString.filter(_ != '-')
    def newReferencedFileDirectoryName(executionId: String) = s"$dataDirectoryName/files-${executionId.filter(_ != '-')}-${omr.newUUID}"

  def isOMR(file: File) = file.getName.endsWith(".omr")

  def omrContent(file: File): OMRFormat =
    version(file) match
      case Version.V1 => OMRFormatV1.omrContent(file)
      case Version.V2 => OMRFormatV2.omrContent(file)

  def fileDirectory(file: File): Option[(String, File)] =
    version(file) match
      case Version.V1 =>
        val index = OMRFormatV1.omrContent(file)
        OMRFormatV1.fileDirectory(file, index)
      case Version.V2 =>
        val index = OMRFormatV2.omrContent(file)
        OMRFormatV2.fileDirectory(file, index)

  def copy(omrFile: File, destination: File) =
    version(omrFile) match
      case Version.V1 => OMRFormatV1.copy(omrFile, destination)
      case Version.V2 => OMRFormatV2.copy(omrFile, destination)

  def move(omrFile: File, destination: File) =
    version(omrFile) match
      case Version.V1 => OMRFormatV1.move(omrFile, destination)
      case Version.V2 => OMRFormatV2.move(omrFile, destination)

  def delete(omrFile: File) =
    version(omrFile) match
      case Version.V1 => OMRFormatV1.delete(omrFile)
      case Version.V2 => OMRFormatV2.delete(omrFile)

  def diskUsage(omrFile: File) =
    version(omrFile) match
      case Version.V1 => OMRFormatV1.diskUsage(omrFile)
      case Version.V2 => OMRFormatV2.diskUsage(omrFile)

  def embedable(omrFile: File): Embedable =
    version(omrFile) match
      case Version.V1 => OMRFormatV1.embedable(omrFile)
      case Version.V2 => OMRFormatV2.embedable(omrFile)

  def variables(embedable: Embedable) =
    embedable match
      case e: OMRFormatV1.EmbedableContent => OMRFormatV1.variables(e)
      case e: OMRFormatV2.EmbedableContent => OMRFormatV2.variables(e)

  def variables(
    omrFile: File,
    relativePath: Boolean = false,
    fileDirectory: Option[File] = None): Seq[(section: API.SectionData, variables: Seq[Variable[?]])] =
    version(omrFile) match
      case Version.V1 => OMRFormatV1.variables(omrFile, relativePath, fileDirectory)
      case Version.V2 => OMRFormatV2.variables(omrFile, relativePath, fileDirectory)

  def methodName(content: OMRFormat): Option[String] =
    content match
      case content: OMRFormatV1 => OMRFormatV1.methodName(content)
      case content: OMRFormatV2 => OMRFormatV2.methodName(content)

  def archive(tos: TarArchiveOutputStream, f: File) =
    version(f) match
      case Version.V1 => OMRFormatV1.archive(tos, f)
      case Version.V2 => OMRFormatV2.archive(tos, f)

  def exportToCSV(
    file: File,
    destination: File,
    unrollArray: Boolean = true,
    arrayOnRow: Boolean = false,
    gzip: Boolean = false) =
    val variable = variables(file, relativePath = true)

    if variable.size == 1
    then
      CSVFormat.writeVariablesToCSV(
        destination,
        variable.head.variables,
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

  def exportToJSON(
    file: File,
    destination: File) =
    import com.fasterxml.jackson.core.JsonFactory
    import org.json4s.jackson.JsonMethods
    import org.json4s.jackson

    val index = omrContent(file)
    def variablesValues = variables(file, relativePath = true)

    case class JSONContent(
      `openmole-version`: String,
      `execution-id`: String,
      script: Option[Script],
      `time-start`: Long,
      `time-save`: Long,
      method: Option[Json]) derives derivation.ConfiguredCodec

    def jsonData =
      variablesValues.toIterator.map: v =>
        def content: Seq[(String, org.json4s.JValue)] =
          def fileToJSON(f: File) = JString(f.getPath)
          v.section.name.map(n => "name" -> org.json4s.JString(n)).toSeq ++
            Seq("variables" -> variablesToJObject(v.variables, default = Some(anyToJValue), file = Some(fileToJSON)))
        org.json4s.JObject(content.toList)

    def jsonContent =
      JSONContent(
        `openmole-version` = index.openMOLEVersion,
        `execution-id` = index.executionID,
        script = index.script,
        `time-start` = index.timeStart,
        `time-save` = index.timeSave,
        method = index.method
      )

    val renderedContent = org.json4s.jackson.parseJson(jsonContent.asJson.deepDropNullValues.noSpaces).asInstanceOf[org.json4s.JObject]
    destination.withOutputStream: os =>
      val gen = new JsonFactory().createGenerator(os)
      gen.useDefaultPrettyPrinter()
      gen.writeStartObject()

      renderedContent.obj.foreach:
        case (name, value) =>
          gen.writeFieldName(name)
          JsonMethods.mapper.writeValue(gen, jackson.renderJValue(value))

      gen.writeFieldName("data")
      gen.writeStartArray()

      jsonData.foreach: obj =>
        JsonMethods.mapper.writeValue(gen, jackson.renderJValue(obj))

      gen.writeEndArray()
      gen.writeEndObject()
      gen.close()


sealed trait OMRFormat
export OMRFormatV2.APIV2.{OutputContent, SectionContent}
export OMRFormatV2.{DataMode => SectionDataMode}

object OMRFormatV1:
  import OMRFormat.*
  def omrVersion = "1.0"

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

  enum DataStore derives derivation.ConfiguredCodec:
    case GZipFile

  object DataContent:
    case class SectionData(name: Option[String], variables: Seq[ValData], indexes: Option[Seq[String]] = None) derives derivation.ConfiguredCodec

  case class DataContent(section: Seq[DataContent.SectionData]) derives derivation.ConfiguredCodec

  case class EmbedableContent(omrFile: File, sectionFile: File, fileDirectory: Option[File]) extends Embedable

  object APIV1:
    case class OMROption(script: Boolean = true, overwrite: Boolean = true, append: Boolean = false, replace: Boolean = false)

    object SectionContent:
      given Conversion[(String, Seq[Variable[?]]), SectionContent] = s => SectionContent(Some(s._1), s._2)
      given Conversion[Seq[Variable[?]], SectionContent] = s => SectionContent(None, s)

    case class SectionContent(name: Option[String], variables: Seq[Variable[?]])
    case class OutputContent(section: SectionContent*)

  def toAPISectionData(content: DataContent.SectionData): API.SectionData = API.SectionData(content.name, content.variables)

  def dataFileName(executionId: String, uuid: String) =
    def executionPrefix = executionId.filter(_ != '-')
    s"$dataDirectoryName/$executionPrefix-$uuid.omd"

  def dataFiles(omrFile: File): Seq[(String, File)] =
    val parent = omrFile.getParentFile
    OMRFormatV1.dataFileNames(omrFile).map: n =>
      (n, parent /  n)

  def dataFile(omrFile: File, index: OMRFormatV1): File = omrFile.getParentFile / index.`data-file`.last

  def dataFile(omrFile: File): File = OMRFormatV1.dataFile(omrFile, omrContent(omrFile))

  def dataFileNames(omrFile: File): Seq[String] = omrContent(omrFile).`data-file`

  def dataDirectory(file: File) = file.getParentFile / dataDirectoryName

  def fileDirectory(omrFile: File, index: OMRFormatV1): Option[(String, File)] = index.`file-directory`.map(d => (d, omrFile.getParentFile / d))

  def readDataStream[T](dataFile: File)(f: java.io.InputStream => T) = dataFile.withGzippedInputStream(f)

  def omrContent(file: File): OMRFormatV1 =
    val content = file.content(gz = true)
    decode[OMRFormatV1](content).toTry.get

  def variablesFromStream(
    omrFile: File,
    is: java.io.InputStream,
    fileDirectory: Option[File]): Seq[(section: API.SectionData, variables: Seq[Variable[?]])] =
    val index = omrContent(omrFile)

    def loadFile(v: org.json4s.JValue) =
      import org.openmole.core.json.*
      v match
        case jv: org.json4s.JString =>
          fileDirectory match
            case Some(fileDirectory) => fileDirectory / jv.s
            case _ => File(jv.s)
        case _ => cannotConvertFromJSON[File](v)

    index.`data-mode` match
      case OMRFormatV1.DataMode.Create =>
        def sectionToVariables(section: API.SectionData, a: JArray) =
          val variables =
            (section.variables zip a.arr).map: (v, j) =>
              jValueToVariable(j, ValData.toVal(v), file = Some(loadFile), default = Some(jValueToAny))

          (section, variables)

        def readContent(): JArray =
          jsonParser.parse(is).asInstanceOf[JArray]

        val content = readContent()

        (index.`data-content`.section zip content.arr).map: (s, c) =>
          sectionToVariables(toAPISectionData(s), c.asInstanceOf[JArray])
      case OMRFormatV1.DataMode.Append =>
        def sectionToAggregatedVariables(section: Seq[API.SectionData], content: JsonParser) =
          val sectionVals =
            section.map: s =>
              s.variables.toArray.map(ValData.toVal)
            .toArray

          val sectionsContent =
            section.toArray.map: s =>
              val size = s.variables.size
              Array.fill(size)(scala.collection.mutable.ArrayBuffer[Any]())

          content.nextToken()
          while content.nextToken() != JsonToken.END_ARRAY
          do
            var sectionIndex = 0
            while content.nextToken() != JsonToken.END_ARRAY
            do
              val values = objectMapper.readValues(content, classOf[JValue]).nextValue().asInstanceOf[JArray]
              values.arr.zipWithIndex.foreach: (v, i) =>
                val vv = sectionVals(sectionIndex)(i)
                sectionsContent(sectionIndex)(i) += jValueToVariable(v, vv, file = Some(loadFile), default = Some(jValueToAny)).value

            sectionIndex += 1

          section.zipWithIndex.map: (s, i) =>
            val variables =
              (s.variables zip sectionsContent(i)).map: (v, a) =>
                Variable.constructArray(ValData.toVal(v).array, a.toSeq, (v, _) => v)

            (s, variables)

        val begin = new StringInputStream("[")
        val end = new StringInputStream("]")
        val s = inputStreamSequence(begin, is, end)

        val p = objectMapper.createParser(s)
        try sectionToAggregatedVariables(index.`data-content`.section.map(toAPISectionData), p)
        finally p.close()

  def writeOMRContent(file: File, content: OMRFormatV1) =
    val jsonContent = content.asJson.mapObject(_.add(OMRFormat.formatVersionField, Json.fromString(omrVersion))).deepDropNullValues

    file.withPrintStream(create = true, gz = true)(
      _.print(jsonContent.noSpaces)
    )

  def write(
    data: APIV1.OutputContent,
    methodFile: File,
    executionId: String,
    jobId: Long,
    methodJson: Json,
    script: Option[Script],
    timeStart: Long,
    openMOLEVersion: String,
    option: APIV1.OMROption)(using TimeService, FileService, TmpDirectory) =

    def methodFormat(existingData: Seq[String], fileName: String, dataContent: OMRFormatV1.DataContent, fileDirectory: Option[String]) =
      def mode =
        if option.append
        then OMRFormatV1.DataMode.Append
        else OMRFormatV1.DataMode.Create

      OMRFormatV1(
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

    val directory = methodFile.getParentFile

    val existingContent =
      if methodFile.exists()
      then
        val contentExecutionId = readSingleJSONField(methodFile, "execution-id", gz = true).get
        if option.overwrite && contentExecutionId != executionId || option.replace
        then
          delete(methodFile)
          None
        else
          val content = omrContent(methodFile)
          Some(content)
      else None

    val existingData = existingContent.toSeq.flatMap(_.`data-file`)

    val resultFileDirectoryName =
      def name = omr.newReferencedFileDirectoryName(executionId)

      existingContent match
        case Some(c) => c.`file-directory`.getOrElse(name)
        case None => name

    val storeFileDirectory = directory / resultFileDirectoryName

    def storeFile(f: File) =
      val destinationPath = s"${summon[FileService].hashNoCache(f)}/${f.getName}"
      f.copy(storeFileDirectory / destinationPath)
      org.json4s.JString(destinationPath)

    def jsonContent = JArray(data.section.map { s => JArray(variablesToJValues(s.variables, default = Some(anyToJValue), file = Some(storeFile)).toList) }.toList)

    val fileName =
      if !option.append
      then s"${dataFileName(executionId, omr.newUUID)}"
      else
        existingData.headOption match
          case Some(h) => h
          case None => s"${dataFileName(executionId, omr.newUUID)}"

    val dataFile = directory / fileName

    dataFile.withPrintStream(append = option.append, create = true, gz = true): ps =>
      if option.append && existingData.nonEmpty then ps.print(",\n")
      ps.print(compact(render(jsonContent)))

    def contentData =
      OMRFormatV1.DataContent:
        data.section.map: s =>
          OMRFormatV1.DataContent.SectionData(s.name, s.variables.map(v => ValData(v.prototype)))

    // Is created by variablesToJValues if it found some files
    def fileDirectoryValue =
      if storeFileDirectory.exists()
      then Some(resultFileDirectoryName)
      else None

    OMRFormatV1.writeOMRContent(
      methodFile,
      methodFormat(existingData, fileName, contentData, fileDirectoryValue)
    )

  def copy(omrFile: File, destination: File) =
    if omrFile != destination
    then
      val originDirectory = omrFile.getParentFile
      val destinationDirectory = destination.getParentFile

      val index = omrContent(omrFile)

      val copiedDataFiles =
        index.`data-file`.map: f =>
          val copiedName = dataFileName(index.`execution-id`, omr.newUUID)
          val copiedFile = destinationDirectory / copiedName

          (originDirectory / f) copy copiedFile
          copiedName

      val copiedReferencedFileDirectory =
        index.`file-directory`.map: d =>
          val copiedDirectory = omr.newReferencedFileDirectoryName(index.`execution-id`)
          (originDirectory / d) copy (destinationDirectory / copiedDirectory)
          copiedDirectory

      OMRFormatV1.writeOMRContent(
        destination,
        index.copy(`data-file` = copiedDataFiles, `file-directory` = copiedReferencedFileDirectory)
      )

  def move(omrFile: File, destination: File) =
    val originDirectory = omrFile.getParentFile
    val destinationDirectory = destination.getParentFile
    val moveData = originDirectory != destinationDirectory
    if moveData
    then
      val destinationDataDirectory = destination.getParentFile
      val index = omrContent(omrFile)
      index.`file-directory`.foreach(d => (originDirectory / d).move(destinationDirectory / d))
      OMRFormatV1.dataFiles(omrFile).foreach((name, file) => file.move(destinationDataDirectory / name))
      val omrDataDirectory = OMRFormatV1.dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    omrFile move destination

  def delete(omrFile: File) =
    try
      OMRFormatV1.dataFiles(omrFile).foreach((_, file) => file.delete())
      fileDirectory(omrFile, omrContent(omrFile)).foreach(_._2.recursiveDelete)
      val omrDataDirectory = OMRFormatV1.dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    finally
      omrFile.delete()

  def diskUsage(omrFile: File) =
    omrFile.size +
      OMRFormatV1.dataFiles(omrFile).map((_, file) => file.size).sum +
      fileDirectory(omrFile, omrContent(omrFile)).map(_._2.size).getOrElse(0L)

  def embedable(omrFile: File) =
    OMRFormatV1.EmbedableContent(omrFile, OMRFormatV1.dataFile(omrFile), fileDirectory(omrFile, omrContent(omrFile)).map(_._2))

  def variables(embedableContent: OMRFormatV1.EmbedableContent) =
    OMRFormatV1.readDataStream(embedableContent.sectionFile): is =>
      OMRFormatV1.variablesFromStream(embedableContent.omrFile, is, embedableContent.fileDirectory)

  def variables(
    omrFile: File,
    relativePath: Boolean = false,
    fileDirectory: Option[File] = None): Seq[(section: API.SectionData, variables: Seq[Variable[?]])] =
    val index = omrContent(omrFile)

    def dataFileValue = OMRFormatV1.dataFile(omrFile, index)

    def fileValue =
      fileDirectory orElse :
        if !relativePath
        then OMRFormatV1.fileDirectory(omrFile, index).map(_._2)
        else None

    OMRFormatV1.readDataStream(dataFileValue): is =>
      OMRFormatV1.variablesFromStream(omrFile, is, fileValue)

  def methodName(content: OMRFormatV1): Option[String] =
    content.method.flatMap: j =>
      j.hcursor.downField(methodNameField).as[String].toOption

  def archive(tos: TarArchiveOutputStream, f: File) =
    tos.addFile(f, f.getName)
    OMRFormatV1.dataFiles(f).foreach((n, df) => tos.addFile(df, n))
    OMRFormatV1.fileDirectory(f, omrContent(f)).foreach((n, f) => tos.addDirectory(f, prefix = n))


case class OMRFormatV1(
  `openmole-version`: String,
  `execution-id`: String,
  `data-file`: Seq[String],
  `data-mode`: OMRFormatV1.DataMode,
  `data-content`: OMRFormatV1.DataContent,
  `data-store`: Option[OMRFormatV1.DataStore] = None,
  `file-directory`: Option[String],
  script: Option[OMRFormat.Script],
  `time-start`: Long,
  `time-save`: Long,
  method: Option[Json]) extends OMRFormat derives derivation.ConfiguredCodec

object OMRFormatV2:
  import OMRFormat.*
  def omrVersion = "2.0"

  object DataMode:
    given Encoder[DataMode] = Encoder.instance:
      case DataMode.Append => Encoder.encodeString("append")
      case DataMode.Update => Encoder.encodeString("update")

  given Decoder[DataMode] =
    Decoder.decodeString.map:
      case "append" => DataMode.Append
      case "update" => DataMode.Update

  enum DataMode:
    case Append, Update

  enum DataStore derives derivation.ConfiguredCodec:
    case GZipFile

  object DataContent:
    case class Section(name: Option[String], variables: Seq[ValData], `data-file`: String, `data-mode`: DataMode) derives derivation.ConfiguredCodec

  case class DataContent(section: Seq[DataContent.Section]) derives derivation.ConfiguredCodec

  case class EmbedableContent(omrFile: File, sections: Seq[File], fileDirectory: Option[File]) extends Embedable

  object APIV2:
    object SectionContent:
      given Conversion[(String, Seq[Variable[?]]), SectionContent] = s => SectionContent(Some(s._1), s._2, DataMode.Update)
      given Conversion[Seq[Variable[?]], SectionContent] = s => SectionContent(None, s, DataMode.Update)

    case class SectionContent(name: Option[String], variables: Seq[Variable[?]], mode: DataMode = DataMode.Update)

    case class OutputContent(section: SectionContent*)

  def dataFileName(executionId: String, uuid: String, sectionIndex: Int) =
    def executionPrefix = executionId.filter(_ != '-')
    s"$dataDirectoryName/$executionPrefix-$uuid-$sectionIndex.omd"

  def toAPISectionData(content: DataContent.Section): API.SectionData = API.SectionData(content.name, content.variables)


  def dataFiles(omrFile: File): Seq[(String, File)] =
    val parent = omrFile.getParentFile
    val index = omrContent(omrFile)
    index.`data-content`.section.map: s =>
      (s.`data-file`, omrFile.getParentFile / s.`data-file`)

  def dataFile(omrFile: File, section: DataContent.Section): File = omrFile.getParentFile / section.`data-file`

  def dataDirectory(file: File) = file.getParentFile / dataDirectoryName

  def fileDirectory(omrFile: File, index: OMRFormatV2): Option[(String, File)] = index.`file-directory`.map(d => (d, omrFile.getParentFile / d))

  def readDataStream[T](dataFile: File)(f: java.io.InputStream => T) = dataFile.withGzippedInputStream(f)

  def omrContent(file: File): OMRFormatV2 =
    val content = file.content(gz = true)
    decode[OMRFormatV2](content).toTry.get

  def sectionVariablesFromStream(
   section: DataContent.Section,
   is: java.io.InputStream,
   fileDirectory: Option[File]): (section: API.SectionData, variables: Seq[Variable[?]]) =

    def loadFile(v: org.json4s.JValue) =
      import org.openmole.core.json.*
      v match
        case jv: org.json4s.JString =>
          fileDirectory match
            case Some(fileDirectory) => fileDirectory / jv.s
            case _ => File(jv.s)
        case _ => cannotConvertFromJSON[File](v)

    section.`data-mode` match
      case DataMode.Update =>
        def sectionToVariables(section: API.SectionData, a: JArray) =
          val variables =
            (section.variables zip a.arr).map: (v, j) =>
              jValueToVariable(j, ValData.toVal(v), file = Some(loadFile), default = Some(jValueToAny))

          (section, variables)

        def readContent(): JArray =
          jsonParser.parse(is).asInstanceOf[JArray]

        val content = readContent()

        sectionToVariables(toAPISectionData(section), content)
      case DataMode.Append =>
        def sectionToAggregatedVariables(section: API.SectionData, content: JsonParser) =
          val sectionVals = section.variables.toArray.map(ValData.toVal)
          val sectionContent =
            val size = section.variables.size
            Array.fill(size)(scala.collection.mutable.ArrayBuffer[Any]())

          content.nextToken()

          while content.nextToken() != JsonToken.END_ARRAY
          do
            val values = objectMapper.readValues(content, classOf[JValue]).nextValue().asInstanceOf[JArray]
            values.arr.zipWithIndex.foreach: (v, i) =>
              val vv = sectionVals(i)
              sectionContent(i) += jValueToVariable(v, vv, file = Some(loadFile), default = Some(jValueToAny)).value


          val variables =
            (section.variables zip sectionContent).map: (v, a) =>
              Variable.constructArray(ValData.toVal(v).array, a.toSeq, (v, _) => v)

          (section, variables)

        val begin = new StringInputStream("[")
        val end = new StringInputStream("]")
        val s = inputStreamSequence(begin, is, end)

        val p = objectMapper.createParser(s)
        try sectionToAggregatedVariables(toAPISectionData(section), p)
        finally p.close()

  def writeOMRContent(file: File, content: OMRFormatV2) =
    val jsonContent =
      val versionFieldValue = (OMRFormat.formatVersionField, Json.fromString(omrVersion))
      content.asJson.mapObject(versionFieldValue +: _).deepDropNullValues

    file.withPrintStream(create = true, gz = true)(
      _.print(jsonContent.noSpaces)
    )

  def write(
    data: APIV2.OutputContent,
    omrFile: File,
    executionId: String,
    jobId: Long,
    methodJson: Json,
    script: Option[Script],
    timeStart: Long,
    openMOLEVersion: String)(using TimeService, FileService, TmpDirectory) =

    def omrFormat(dataContent: OMRFormatV2.DataContent, fileDirectory: Option[String]) =
      OMRFormatV2(
        `openmole-version` = openMOLEVersion,
        `execution-id` = executionId,
        `data-content` = dataContent,
        `file-directory` = fileDirectory,
        script = script,
        `time-start` = timeStart,
        `time-save` = TimeService.currentTime,
        method = Some(methodJson)
      )

    val directory = omrFile.getParentFileSafe

    val existingContent =
      if omrFile.exists()
      then
        val contentExecutionId = readSingleJSONField(omrFile, "execution-id", gz = true).get
        if contentExecutionId != executionId
        then
          delete(omrFile)
          None
        else
          Some(omrContent(omrFile))
      else None

    def fileName(sectionIndex: Int, exitingContent: Option[DataContent.Section]) =
      exitingContent match
        case Some(content) => content.`data-file`
        case None => s"${dataFileName(executionId, omr.newUUID, sectionIndex)}"

    val resultFileDirectoryName =
      def name = omr.newReferencedFileDirectoryName(executionId)

      existingContent match
        case Some(c) => c.`file-directory`.getOrElse(name)
        case None => name

    val storeFileDirectory = directory / resultFileDirectoryName

    def storeFile(f: File) =
      val destinationPath = s"${summon[FileService].hashNoCache(f)}/${f.getName}"
      f.copy(storeFileDirectory / destinationPath)
      org.json4s.JString(destinationPath)

    for
      (s, index) <- data.section.zipWithIndex
    do
      val dataFile = directory / fileName(index, existingContent.map(_.`data-content`.section(index)))
      def jsonContent = JArray(variablesToJValues(s.variables, default = Some(anyToJValue), file = Some(storeFile)).toList)
      def append = s.mode == DataMode.Append
      val dataFileEmpty = dataFile.isEmpty

      dataFile.withPrintStream(append = append, create = true, gz = true): ps =>
        if append && !dataFileEmpty
        then ps.print(",\n")
        ps.print(compact(render(jsonContent)))

    def contentData =
      OMRFormatV2.DataContent:
        data.section.zipWithIndex.map: (s, index) =>
          val dataFileName = fileName(index, existingContent.map(_.`data-content`.section(index)))
          def variables = s.variables.map(v => ValData(v.prototype))
          DataContent.Section(s.name, variables, dataFileName, s.mode)

    // Is created by variablesToJValues if it found some files
    def fileDirectoryValue =
      if storeFileDirectory.exists()
      then Some(resultFileDirectoryName)
      else None

    OMRFormatV2.writeOMRContent(
      omrFile,
      omrFormat(contentData, fileDirectoryValue)
    )

  def copy(omrFile: File, destination: File) =
    if omrFile != destination
    then
      val originDirectory = omrFile.getParentFile
      val destinationDirectory = destination.getParentFile

      val index = omrContent(omrFile)

      val copiedSections =
        index.`data-content`.section.zipWithIndex.map: (s, i) =>
          val copiedName = dataFileName(index.`execution-id`, omr.newUUID, i)
          val copiedFile = destinationDirectory / copiedName

          (originDirectory / s.`data-file`) copy copiedFile
          s.copy(`data-file` = copiedName)

      val copiedReferencedFileDirectory =
        index.`file-directory`.map: d =>
          val copiedDirectory = omr.newReferencedFileDirectoryName(index.`execution-id`)
          (originDirectory / d) copy (destinationDirectory / copiedDirectory)
          copiedDirectory

      OMRFormatV2.writeOMRContent(
        destination,
        index.copy(`data-content` = index.`data-content`.copy(section = copiedSections), `file-directory` = copiedReferencedFileDirectory)
      )

  def move(omrFile: File, destination: File) =
    val originDirectory = omrFile.getParentFile
    val destinationDirectory = destination.getParentFile
    val moveData = originDirectory != destinationDirectory
    if moveData
    then
      val destinationDataDirectory = destination.getParentFile
      val index = omrContent(omrFile)
      index.`file-directory`.foreach(d => (originDirectory / d).move(destinationDirectory / d))
      OMRFormatV2.dataFiles(omrFile).foreach((name, file) => file.move(destinationDataDirectory / name))
      val omrDataDirectory = OMRFormatV2.dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    omrFile move destination

  def delete(omrFile: File) =
    try
      OMRFormatV2.dataFiles(omrFile).foreach((_, file) => file.delete())
      fileDirectory(omrFile, omrContent(omrFile)).foreach(_._2.recursiveDelete)
      val omrDataDirectory = OMRFormatV2.dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    finally
      omrFile.delete()

  def diskUsage(omrFile: File) =
    omrFile.size +
      OMRFormatV2.dataFiles(omrFile).map((_, file) => file.size).sum +
      fileDirectory(omrFile, omrContent(omrFile)).map(_._2.size).getOrElse(0L)

  def embedable(omrFile: File) =
    val index = omrContent(omrFile)
    val sectionFiles = index.`data-content`.section.map(s => dataFile(omrFile, s))
    OMRFormatV2.EmbedableContent(omrFile, sectionFiles, fileDirectory(omrFile, omrContent(omrFile)).map(_._2))

  def variables(embedableContent: OMRFormatV2.EmbedableContent) =
    val index = omrContent(embedableContent.omrFile)
    (index.`data-content`.section zip embedableContent.sections).map: (s, f) =>
      OMRFormatV2.readDataStream(f): is =>
        OMRFormatV2.sectionVariablesFromStream(s, is, embedableContent.fileDirectory)

  def variables(
    omrFile: File,
    relativePath: Boolean = false,
    fileDirectory: Option[File] = None): Seq[(section: API.SectionData, variables: Seq[Variable[?]])] =
    val index = omrContent(omrFile)

    def fileValue =
      fileDirectory orElse :
        if !relativePath
        then OMRFormatV2.fileDirectory(omrFile, index).map(_._2)
        else None

    index.`data-content`.section.map: s =>
      val file = dataFile(omrFile, s)
      OMRFormatV2.readDataStream(file): is =>
        OMRFormatV2.sectionVariablesFromStream(s, is, fileValue)

  def methodName(content: OMRFormatV2): Option[String] =
    content.method.flatMap: j =>
      j.hcursor.downField(methodNameField).as[String].toOption

  def archive(tos: TarArchiveOutputStream, f: File) =
    tos.addFile(f, f.getName)
    OMRFormatV2.dataFiles(f).foreach((n, df) => tos.addFile(df, n))
    OMRFormatV2.fileDirectory(f, omrContent(f)).foreach((n, f) => tos.addDirectory(f, prefix = n))


case class OMRFormatV2(
  `openmole-version`: String,
  `execution-id`: String,
  `data-content`: OMRFormatV2.DataContent,
  `data-store`: Option[OMRFormatV2.DataStore] = None,
  `file-directory`: Option[String],
  script: Option[OMRFormat.Script],
  `time-start`: Long,
  `time-save`: Long,
  method: Option[Json]) extends OMRFormat derives derivation.ConfiguredCodec