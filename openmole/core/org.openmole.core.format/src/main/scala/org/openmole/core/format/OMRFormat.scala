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
import org.openmole.core.context.{Val, ValType, Variable}
import org.openmole.core.exception.*
import org.openmole.core.fileservice.FileService
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

 enum DataStore derives derivation.ConfiguredCodec:
  case GZipFile

 object DataContent:
   case class SectionData(name: Option[String], variables: Seq[ValData], indexes: Option[Seq[String]] = None) derives derivation.ConfiguredCodec

 case class DataContent(section: Seq[DataContent.SectionData])derives derivation.ConfiguredCodec

case class OMRContent(
  `format-version`: String,
  `openmole-version`: String,
  `execution-id`: String,
  `data-file`: Seq[String],
  `data-mode`: OMRContent.DataMode,
  `data-content`: OMRContent.DataContent,
  `data-store`: Option[OMRContent.DataStore] = None,
  `file-directory`: Option[String],
  script: Option[OMRContent.Script],
  `time-start`: Long,
  `time-save`: Long,
  method: Option[Json]) derives derivation.ConfiguredCodec

def methodNameField = "method-name"
def omrVersion = "1.0"
def dataDirectoryName = ".omr-data"

object OMRFormat:
  object omr:
    def dataFileName(executionId: String, uuid: String) =
      def executionPrefix = executionId.filter(_ != '-')
      s"$dataDirectoryName/$executionPrefix-$uuid.omd"

    def newUUID = UUID.randomUUID().toString.filter(_ != '-')

    def writeOMRContent(file: File, content: OMRContent) =
      file.withPrintStream(create = true, gz = true)(
        _.print(content.asJson.deepDropNullValues.noSpaces)
      )

    def newReferencedFileDirectoryName(executionId: String) = s"$dataDirectoryName/files-${executionId.filter(_ != '-')}-${omr.newUUID}"

  def resultFileDirectory(file: File) =
    val index = omrContent(file)
    index.`file-directory`.map(d => file.getParentFile / d)

  def dataDirectory(file: File) =
    file.getParentFile / dataDirectoryName

  def isOMR(file: File) = file.getName.endsWith(".omr")

  def omrContent(file: File): OMRContent =
    val content = file.content(gz = true)
    decode[OMRContent](content).toTry.get

  def dataFiles(file: File): Seq[String] = omrContent(file).`data-file`
  def storeFiles(omrFile: File): Seq[(String, File)] = dataFiles(omrFile).map(n => (n, dataFile(omrFile, n)))

  def dataFile(omrFile: File, name: String) = omrFile.getParentFile / name

  def readDataStream[T](omrFile: File, name: String)(f: java.io.InputStream => T) =
    dataFile(omrFile, name).withGzippedInputStream(f)


  def write(
    data: OutputFormat.OutputContent,
    methodFile: File,
    executionId: String,
    jobId: Long,
    methodJson: Json,
    script: Option[OMRContent.Script],
    timeStart: Long,
    openMOLEVersion: String,
    option: OMROption)(using TimeService, FileService, TmpDirectory) =

    def methodFormat(existingData: Seq[String], fileName: String, dataContent: OMRContent.DataContent, fileDirectory: Option[String]) =
      def mode =
        if option.append
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
      then s"$dataDirectoryName/${omr.dataFileName(executionId, omr.newUUID)}"
      else
        existingData.headOption match
          case Some(h) => h
          case None => s"$dataDirectoryName/${omr.dataFileName(executionId, omr.newUUID)}"

    val dataFile = directory / fileName

    dataFile.withPrintStream(append = option.append, create = true, gz = true): ps =>
      if option.append && existingData.nonEmpty then ps.print(",\n")
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

    omr.writeOMRContent(
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
      storeFiles(omrFile).foreach((name, file) => file.move(destinationDataDirectory / name))
      val omrDataDirectory = dataDirectory(omrFile)
      if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    omrFile move destination

  def delete(omrFile: File) =
    storeFiles(omrFile).foreach((_, file) => file.delete())
    resultFileDirectory(omrFile).foreach(_.recursiveDelete)
    val omrDataDirectory = dataDirectory(omrFile)
    if omrDataDirectory.isEmpty then omrDataDirectory.recursiveDelete
    omrFile.delete()

  def diskUsage(omrFile: File) =
    omrFile.size +
      OMRFormat.storeFiles(omrFile).map((_, file) => file.size).sum +
      OMRFormat.resultFileDirectory(omrFile).map(_.size).getOrElse(0L)

//  def keepLastDataFile(omrFile: File) =
//    val df = dataFiles(omrFile)
//    val keep = df.last
//    val content = omrContent(omrFile)
//    val newContent = content.copy(`data-file` = Seq(keep._1))
//    try writeOMRContent(omrFile, newContent)
//    finally df.dropRight(1).foreach((_, f) => f.delete())

  def variables(
    omrFile: File,
    relativePath: Boolean = false,
    dataFile: Option[String] = None,
    indexOnly: Boolean = false): Seq[(section: OMRContent.DataContent.SectionData, variables: Seq[Variable[?]])] =
    val index = omrContent(omrFile)
    val dataFileValue = dataFile getOrElse index.`data-file`.last
    OMRFormat.readDataStream(omrFile, dataFileValue): is =>
      variablesFromStream(omrFile, is, relativePath, indexOnly = indexOnly)

  def variablesFromStream(
    omrFile: File,
    is: java.io.InputStream,
    relativePath: Boolean = false,
    indexOnly: Boolean = false): Seq[(section: OMRContent.DataContent.SectionData, variables: Seq[Variable[?]])] =
    val index = omrContent(omrFile)
    val omrDirectory = omrFile.getParentFile

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
          lazy val isIndex = section.indexes.getOrElse(Seq()).toSet
          def indexFilter(v: ValData) = if !indexOnly then true else isIndex.contains(v.name)

          val variables =
            (section.variables zip a.arr).filter((v, _) => indexFilter(v)).map: (v, j) =>
              jValueToVariable(j, ValData.toVal(v), file = Some(loadFile), default = Some(jValueToAny))

          (section, variables)

        def readContent(): JArray =
          import org.json4s.jackson.JsonMethods.*
          parse(is).asInstanceOf[JArray]

        val content = readContent()

        (index.`data-content`.section zip content.arr).map: (s, c) =>
          sectionToVariables(s, c.asInstanceOf[JArray])
      case OMRContent.DataMode.Append =>
        def sectionToAggregatedVariables(section: OMRContent.DataContent.SectionData, sectionIndex: Int, content: JArray) =
          val size = section.variables.size
          val sectionContent = content.arr.map(a => a.asInstanceOf[JArray].arr(sectionIndex))

          def transposed = (0 until size).map { i => JArray(sectionContent.map(_.asInstanceOf[JArray](i))) }

          lazy val isIndex = section.indexes.getOrElse(Seq()).toSet
          def indexFilter(v: ValData) = if !indexOnly then true else isIndex.contains(v.name)

          val variables =
            (section.variables zip transposed).filter((v, _) => indexFilter(v)).map: (v, j) =>
              jValueToVariable(j, ValData.toVal(v).toArray, file = Some(loadFile), default = Some(jValueToAny))

          (section, variables)
        def readContent(): JArray =
          val begin = new StringInputStream("[")
          val end = new StringInputStream("]")
          val s = inputStreamSequence(begin, is, end)
          import org.json4s.jackson.JsonMethods.*
          parse(s).asInstanceOf[JArray]

        val content = readContent()
        index.`data-content`.section.zipWithIndex.map: (s, i) =>
          sectionToAggregatedVariables(s, i, content)


  object IndexedData:
    type FileIndex = String

  case class IndexedData(variableName: String, sectionIndex: Int, values: Array[Any], fileIndex: Array[IndexedData.FileIndex])

  def indexes(file: File): Seq[IndexedData] =
    val content = omrContent(file)
    val indexes = content.`data-content`.section.zipWithIndex.flatMap((s, i) => s.indexes.toSeq.flatten.map(id => i -> id))

    val indexedValues = indexes.map(i => i -> new collection.mutable.ArrayBuffer[(IndexedData.FileIndex, Any)](content.`data-file`.size)).toMap

    for
      fileName <- dataFiles(file)
      sectionIndexes = variables(file, dataFile = Some(fileName), indexOnly = true)
      ((section, variables), i) <- sectionIndexes.zipWithIndex
      v <- variables
    do
      indexedValues((i, v.name)) += ((fileName, v.value))

    indexes.map: i =>
      val values = indexedValues(i).toArray
      IndexedData(i._2, i._1, values.map(_._2), values.map(_._1))

  def variablesAtIndex(file: File, index: IndexedData.FileIndex) =
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
    gzip: Boolean = false) =
    val variable = variables(file, relativePath = true, dataFile = dataFile)

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

  def writeJSON(
    file: File,
    destination: File,
    dataFile: Option[String] = None) =

    val index = omrContent(file)
    def variablesValues = variables(file, relativePath = true, dataFile = dataFile)

    case class JSONContent(
      `openmole-version`: String,
      `execution-id`: String,
      script: Option[OMRContent.Script],
      `time-start`: Long,
      `time-save`: Long,
      method: Option[Json]) derives derivation.ConfiguredCodec

    import OMRContent.given

    def jsonData =
      org.json4s.JArray(
        variablesValues.map: v =>
          def content: Seq[(String, org.json4s.JValue)] =
            def fileToJSON(f: File) = JString(f.getPath)
            v.section.name.map(n => "name" -> org.json4s.JString(n)).toSeq ++
              Seq("variables" -> variablesToJObject(v.variables, default = Some(anyToJValue), file = Some(fileToJSON)))
          org.json4s.JObject(content.toList)
        .toList
      )

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
      import org.json4s.jackson
      val fullObject = renderedContent.copy(obj = renderedContent.obj ++ Seq("data" -> jsonData))
      val writer = jackson.JsonMethods.mapper.writerWithDefaultPrettyPrinter()
      writer.writeValue(os, jackson.renderJValue(fullObject))

  def readSingleJSONField(file: File, targetField: String): Option[String] =
    import com.fasterxml.jackson.core.JsonFactory
    import scala.util.boundary

    val factory = JsonFactory()
    file.withGzippedInputStream: st =>
      val parser = factory.createParser(st)
      try
        boundary[Option[String]]:
          while parser.nextToken() != null
          do
            if parser.currentName() == targetField
            then
              parser.nextToken()
              boundary.break(Some(parser.getValueAsString))
          None
      finally
        parser.close()
