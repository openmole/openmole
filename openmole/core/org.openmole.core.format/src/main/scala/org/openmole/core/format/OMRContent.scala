package org.openmole.core.format

/*
 * Copyright (C) 2026 Romain Reuillon
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

object OMRContent:
  case class Import(`import`: String, content: String) derives derivation.ConfiguredCodec
  case class Script(content: String, `import`: Option[Seq[Import]]) derives derivation.ConfiguredCodec

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




