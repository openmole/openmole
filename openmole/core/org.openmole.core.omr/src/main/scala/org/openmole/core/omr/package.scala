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
import org.openmole.core.json.*
import org.openmole.core.context.Variable
import org.openmole.core.omr.*
import org.openmole.core.exception.*
import org.openmole.tool.stream.{StringInputStream, inputStreamSequence}
import org.openmole.tool.file.*

implicit val omrCirceDefault: io.circe.derivation.Configuration =
 io.circe.derivation.Configuration.default.withKebabCaseMemberNames.withDefaults.withDiscriminator("type")

object Index:
 given Codec[Import] = Codec.AsObject.derivedConfigured
 given Codec[Script] = Codec.AsObject.derivedConfigured
 given Codec[Index] = Codec.AsObject.derivedConfigured

 case class Import(`import`: String, content: String)
 case class Script(content: String, `import`: Option[Seq[Import]])

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
 `data-compression`: Option[Index.Compression],
 script: Option[Index.Script],
 `time-start`: Long,
 `time-save`: Long)

def methodField = "method"
def methodPluginField = "plugin"
def omrVersion = "0.2"
def dataDirectory = ".omr-data"


object OMR:
  def indexData(file: File): Index =
    val content = file.content(gz = true)
    decode[Index](content).toTry.get

  def toVariables(file: File): Seq[(DataContent.SectionData, Seq[Variable[_]])] =
    val index = indexData(file)
    val data: File = file.getParentFile / index.`data-file`.last

    index.`data-mode` match
      case Index.DataMode.Create =>
        def sectionToVariables(section: DataContent.SectionData, a: JArray) =
          section -> (section.variables zip a.arr).map { (v, j) => jValueToVariable(j, ValData.toVal(v)) }

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
          section -> (section.variables zip transposed).map { (v, j) => jValueToVariable(j, ValData.toVal(v).toArray) }

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

  def methodName(file: File): String =
    val j = parse(file.content(gz = true)).toTry.get
    j.hcursor.
      downField(methodField).
      downField( methodPluginField).as[String].
      toTry.get

  def writeCSV(
    file: File,
    destination: File,
    unrollArray: Boolean = true,
    arrayOnRow: Boolean = false,
    gzip: Boolean = false) =
    import org.openmole.core.csv
    val variable = toVariables(file)

    if variable.size == 1
    then
      csv.writeVariablesToCSV(
        destination,
        variable.head._2,
        unrollArray = unrollArray,
        arrayOnRow = arrayOnRow,
        gzip = gzip)
    else
      destination.delete()
      for
        ((section, v), i) <- variable.zipWithIndex
      do
        destination.append(s"#section: ${section.name.getOrElse(i.toString)}")
        csv.writeVariablesToCSV(
          destination,
          v,
          unrollArray = unrollArray,
          arrayOnRow = arrayOnRow,
          gzip = gzip,
          append = true
        )