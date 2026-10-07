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
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.stream.{DefaultBufferSize, StringInputStream, inputStreamSequence}
import org.openmole.tool.file.*

import java.util
import java.util.UUID
import scala.collection.mutable

object OMRFormat:
  def methodNameField = "method-name"
  def dataDirectoryName = ".omr-data"
  def omrVersion = "1.0"

  case class Import(`import`: String, content: String) derives derivation.ConfiguredCodec
  case class Script(content: String, `import`: Option[Seq[Import]]) derives derivation.ConfiguredCodec

  object SectionData:
    def apply(section: V1.DataContent.SectionData) = new OMRFormat.SectionData(section.name, section.variables, section.indexes)

  case class SectionData(name: Option[String], variables: Seq[ValData], indexes: Option[Seq[String]] = None) derives derivation.ConfiguredCodec

  object V1:

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

    case class EmbedableContent(omrFile: File, sectionFile: File, fileDirectory: Option[File])


  case class V1(
    `format-version`: String,
    `openmole-version`: String,
    `execution-id`: String,
    `data-file`: Seq[String],
    `data-mode`: V1.DataMode,
    `data-content`: V1.DataContent,
    `data-store`: Option[V1.DataStore] = None,
    `file-directory`: Option[String],
    script: Option[Script],
    `time-start`: Long,
    `time-save`: Long,
    method: Option[Json]) derives derivation.ConfiguredCodec

  object omr:
    def dataFileName(executionId: String, uuid: String) =
      def executionPrefix = executionId.filter(_ != '-')
      s"$dataDirectoryName/$executionPrefix-$uuid.omd"

    def newUUID = UUID.randomUUID().toString.filter(_ != '-')

    def writeOMRContent(file: File, content: V1) =
      file.withPrintStream(create = true, gz = true)(
        _.print(content.asJson.deepDropNullValues.noSpaces)
      )

    def newReferencedFileDirectoryName(executionId: String) = s"$dataDirectoryName/files-${executionId.filter(_ != '-')}-${omr.newUUID}"

    def dataFile(omrFile: File, index: V1): File = omrFile.getParentFile / index.`data-file`.last
    def dataFile(omrFile: File, name: String): File = omrFile.getParentFile / name
    def dataFile(omrFile: File): File = omr.dataFile(omrFile, omrContent(omrFile))
    def dataFileNames(omrFile: File): Seq[String] = omrContent(omrFile).`data-file`
    def dataDirectory(file: File) = file.getParentFile / dataDirectoryName
    def fileDirectory(omrFile: File, index: V1): Option[File] = index.`file-directory`.map(d => omrFile.getParentFile / d)
    def readDataStream[T](dataFile: File)(f: java.io.InputStream => T) = dataFile.withGzippedInputStream(f)

    def variablesFromStream(
      omrFile: File,
      is: java.io.InputStream,
      fileDirectory: Option[File]): Seq[(section: SectionData, variables: Seq[Variable[?]])] =
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
        case V1.DataMode.Create =>
          def sectionToVariables(section: SectionData, a: JArray) =
            lazy val isIndex = section.indexes.getOrElse(Seq()).toSet

            val variables =
              (section.variables zip a.arr).map: (v, j) =>
                jValueToVariable(j, ValData.toVal(v), file = Some(loadFile), default = Some(jValueToAny))

            (section, variables)

          def readContent(): JArray =
            jsonParser.parse(is).asInstanceOf[JArray]

          val content = readContent()

          (index.`data-content`.section zip content.arr).map: (s, c) =>
            sectionToVariables(SectionData(s), c.asInstanceOf[JArray])
        case V1.DataMode.Append =>
          def sectionToAggregatedVariables(section: Seq[SectionData], content: JsonParser) =
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
              lazy val isIndex = s.indexes.getOrElse(Seq()).toSet
              val variables =
                (s.variables zip sectionsContent(i)).map: (v, a) =>
                  Variable.constructArray(ValData.toVal(v).array, a.toSeq, (v, _) => v)

              (s, variables)

          val begin = new StringInputStream("[")
          val end = new StringInputStream("]")
          val s = inputStreamSequence(begin, is, end)

          val p = objectMapper.createParser(s)
          try sectionToAggregatedVariables(index.`data-content`.section.map(SectionData.apply), p)
          finally p.close()

  def isOMR(file: File) = file.getName.endsWith(".omr")

  def omrContent(file: File): V1 =
    val content = file.content(gz = true)
    decode[V1](content).toTry.get

  def dataFiles(omrFile: File): Seq[(String, File)] =
    omr.dataFileNames(omrFile).map: n =>
      (n, omr.dataFile(omrFile, n))

  def fileDirectory(file: File): Option[File] =
    val index = omrContent(file)
    omr.fileDirectory(file, index)

  def write(
    data: OutputFormat.OutputContent,
    methodFile: File,
    executionId: String,
    jobId: Long,
    methodJson: Json,
    script: Option[Script],
    timeStart: Long,
    openMOLEVersion: String,
    option: OMROption)(using TimeService, FileService, TmpDirectory) =

    def methodFormat(existingData: Seq[String], fileName: String, dataContent: V1.DataContent, fileDirectory: Option[String]) =
      def mode =
        if option.append
        then V1.DataMode.Append
        else V1.DataMode.Create

      V1(
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

    val directory = methodFile.getParentFile

    val existingContent =
      if methodFile.exists()
      then
        val contentExecutionId = readSingleJSONField(methodFile, "execution-id").get
        if option.overwrite && contentExecutionId != executionId || option.replace
        then
          OMRFormat.delete(methodFile)
          None
        else
          val content = OMRFormat.omrContent(methodFile)
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
      then s"${omr.dataFileName(executionId, omr.newUUID)}"
      else
        existingData.headOption match
          case Some(h) => h
          case None => s"${omr.dataFileName(executionId, omr.newUUID)}"

    val dataFile = directory / fileName

    dataFile.withPrintStream(append = option.append, create = true, gz = true): ps =>
      if option.append && existingData.nonEmpty then ps.print(",\n")
      ps.print(compact(render(jsonContent)))

    def contentData =
      V1.DataContent:
        data.section.map: s =>
          def sectionIndex = if s.indexes.nonEmpty then Some(s.indexes) else None
          V1.DataContent.SectionData(s.name, s.variables.map(v => ValData(v.prototype)), sectionIndex)

    // Is created by variablesToJValues if it found some files
    def fileDirectoryValue =
      if storeFileDirectory.exists()
      then Some(resultFileDirectoryName)
      else None

    omr.writeOMRContent(
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
          val copiedName = omr.dataFileName(index.`execution-id`, omr.newUUID)
          val copiedFile = destinationDirectory / copiedName

          (originDirectory / f) copy copiedFile
          copiedName

      val copiedReferencedFileDirectory =
        index.`file-directory`.map: d =>
          val copiedDirectory = omr.newReferencedFileDirectoryName(index.`execution-id`)
          (originDirectory / d) copy (destinationDirectory / copiedDirectory)
          copiedDirectory

      omr.writeOMRContent(
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
      dataFiles(omrFile).foreach((name, file) => file.move(destinationDataDirectory / name))
      val omrDataDirectory = omr.dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    omrFile move destination

  def delete(omrFile: File) =
    try
      dataFiles(omrFile).foreach((_, file) => file.delete())
      fileDirectory(omrFile).foreach(_.recursiveDelete)
      val omrDataDirectory = omr.dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    finally
      omrFile.delete()

  def diskUsage(omrFile: File) =
    omrFile.size +
      OMRFormat.dataFiles(omrFile).map((_, file) => file.size).sum +
      OMRFormat.fileDirectory(omrFile).map(_.size).getOrElse(0L)

  def embedable(omrFile: File) =
    V1.EmbedableContent(omrFile, omr.dataFile(omrFile), fileDirectory(omrFile))

  def variables(embedableContent: V1.EmbedableContent) =
    omr.readDataStream(embedableContent.sectionFile): is =>
      omr.variablesFromStream(embedableContent.omrFile, is, embedableContent.fileDirectory)

  def variables(
    omrFile: File,
    relativePath: Boolean = false,
    fileDirectory: Option[File] = None): Seq[(section: SectionData, variables: Seq[Variable[?]])] =
    val index = omrContent(omrFile)
    
    def dataFileValue = omr.dataFile(omrFile, index)

    def fileValue =
      fileDirectory orElse:
        if !relativePath
        then omr.fileDirectory(omrFile, index)
        else None

    omr.readDataStream(dataFileValue): is =>
      omr.variablesFromStream(omrFile, is, fileValue)

  def methodName(file: File): Option[String] =
    methodName(omrContent(file))

  def methodName(content: V1): Option[String] =
    content.method.flatMap: j =>
      j.hcursor.downField(methodNameField).as[String].toOption

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

    import V1.given

    def jsonData =
      variablesValues.toIterator.map: v =>
        def content: Seq[(String, org.json4s.JValue)] =
          def fileToJSON(f: File) = JString(f.getPath)
          v.section.name.map(n => "name" -> org.json4s.JString(n)).toSeq ++
            Seq("variables" -> variablesToJObject(v.variables, default = Some(anyToJValue), file = Some(fileToJSON)))
        org.json4s.JObject(content.toList)


    def jsonContent =
      JSONContent(
        `openmole-version` = index.`openmole-version`,
        `execution-id` = index.`execution-id`,
        script = index.script,
        `time-start` = index.`time-start`,
        `time-save` = index.`time-save`,
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
