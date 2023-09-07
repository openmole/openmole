package org.openmole.core.omr

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
import org.openmole.core.omr.*
import org.openmole.core.exception.*
import org.openmole.core.serializer.SerializerService
import org.openmole.tool.stream.{StringInputStream, inputStreamSequence}
import org.openmole.tool.file.*

implicit val omrCirceDefault: io.circe.derivation.Configuration =
 io.circe.derivation.Configuration.default.withKebabCaseMemberNames.withDefaults.withDiscriminator("type").withTransformConstructorNames(derivation.renaming.kebabCase)

object Index:
 case class Import(`import`: String, content: String) derives derivation.ConfiguredCodec
 case class Script(content: String, `import`: Option[Seq[Import]]) derives derivation.ConfiguredCodec

 object DataMode:
  given Encoder[DataMode] = Encoder.instance:
   case DataMode.Append => Encoder.encodeString("append")
   case DataMode.Create => Encoder.encodeString("create")

  given Decoder[DataMode] = Decoder.decodeString.map:
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

case class Index(
 `format-version`: String,
 `openmole-version`: String,
 `execution-id`: String,
 `data-file`: Seq[String],
 `data-mode`: Index.DataMode,
 `data-content`: DataContent,
 `data-compression`: Option[Index.Compression] = None,
 `file-directory`: Option[String],
 script: Option[Index.Script],
 `time-start`: Long,
 `time-save`: Long) derives derivation.ConfiguredCodec

def methodField = "method"
def omrVersion = "1.0"
def dataDirectoryName = ".omr-data"

object OMRFormat:
  def resultFileDirectoryName(executionId: String) =
    s"$dataDirectoryName/files-${executionId.filter(_ != '-')}"

  def resultFileDirectory(file: File) =
    val index = indexData(file)
    index.`file-directory`.map(d => file.getParentFile / d)

  def dataDirectory(file: File) =
    file.getParentFile / dataDirectoryName

  def isOMR(file: File) = file.getName.endsWith(".omr")

  def indexData(file: File): Index =
    val content = file.content(gz = true)
    decode[Index](content).toTry.get

  def dataFiles(file: File): Seq[(String, File)] =
    val directory = file.getParentFile
    indexData(file).`data-file`.map { f => (f, directory / f) }

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
      val index = indexData(omrFile)
      index.`file-directory`.foreach(d => (originDirectory / d).copy(destinationDirectory / d))

    omrFile copy destination

  def move(omrFile: File, destination: File) =
    val originDirectory = omrFile.getParentFile
    val destinationDirectory = destination.getParentFile
    val moveData = originDirectory != destinationDirectory
    if moveData
    then
      val destinationDataDirectory = destination.getParentFile
      val index = indexData(omrFile)
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

  def toVariables(file: File, relativePath: Boolean = false): Seq[(DataContent.SectionData, Seq[Variable[_]])] =
    val index = indexData(file)
    val omrDirectory = file.getParentFile
    val data: File = omrDirectory / index.`data-file`.last

    def loadFile(v: org.json4s.JValue) =
      import org.openmole.core.json.*
      v match
        case jv: org.json4s.JString =>
          index.`file-directory` match
            case Some(fileDirectory) if !relativePath => omrDirectory / fileDirectory / jv.s
            case _ => File(jv.s)
        case _ => cannotConvertFromJSON[File](v)

    index.`data-mode` match
      case Index.DataMode.Create =>
        def sectionToVariables(section: DataContent.SectionData, a: JArray) =
          section -> (section.variables zip a.arr).map { (v, j) => jValueToVariable(j, ValData.toVal(v), file = Some(loadFile)) }

        def readContent(file: File): JArray =
          file.withGzippedInputStream { is =>
            import org.json4s.jackson.JsonMethods.*
            parse(is).asInstanceOf[JArray]
          }

        val content = readContent(data)
        (index.`data-content`.section zip content.arr).map((s, c) => sectionToVariables(s, c.asInstanceOf[JArray]))
      case Index.DataMode.Append =>
        def sectionToAggregatedVariables(section: DataContent.SectionData, sectionIndex: Int, content: JArray) =
          val size = section.variables.size
          val sectionContent = content.arr.map(a => a.asInstanceOf[JArray].arr(sectionIndex))
          def transposed = (0 until size).map { i => JArray(sectionContent.map(_.asInstanceOf[JArray](i))) }
          section -> (section.variables zip transposed).map { (v, j) => jValueToVariable(j, ValData.toVal(v).toArray, file = Some(loadFile)) }

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

  def methodName(file: File): Option[String] =
    val j = parse(file.content(gz = true)).toTry.get
    j.hcursor.downField(methodField).as[String].toOption

  def writeCSV(
    file: File,
    destination: File,
    unrollArray: Boolean = true,
    arrayOnRow: Boolean = false,
    gzip: Boolean = false) =
    import org.openmole.core.csv.*
    val variable = toVariables(file, relativePath = true)

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
    destination: File)(using SerializerService) =

    val index = indexData(file)
    def variables = toVariables(file, relativePath = true)

    case class JSONContent(
      `openmole-version`: String,
      `execution-id`: String,
      script: Option[Index.Script],
      `time-start`: Long,
      `time-save`: Long) derives derivation.ConfiguredCodec

    import Index.given

    def jsonData =
      org.json4s.JArray(
        variables.map { (s, variables) =>
          def content: Seq[(String, org.json4s.JValue)] =
            def fileToJSON(f: File) = JString(f.getPath)
            s.name.map(n => "name" -> org.json4s.JString(n)).toSeq ++
              Seq("variables" -> variablesToJObject(variables, default = Some(anyToJValue), file = Some(fileToJSON)))
          org.json4s.JObject(content.toList)
        }.toList
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
