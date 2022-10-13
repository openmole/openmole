package org.openmole.plugin.tool.methoddata

/*
 * Copyright (C) 2022 Romain Reuillon
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

object ContentData:
  case class Plain(variables: Seq[ValData]) extends ContentData
  case class Section(section: Seq[SectionData]) extends ContentData
  case class SectionData(name: String, variables: Seq[ValData])

  given Encoder[ContentData] = c =>
    c match
      case p: Plain => Encoder.forProduct2[Plain, String, Seq[ValData]]("type", "content") { p => ("plain", p.variables) }.apply(p)
      case s: Section => Encoder.forProduct2[Section, String, Seq[SectionData]]("type", "content") { s => ("section", s.section) }.apply(s)

  given Decoder[ContentData] = j =>
    j.downField("type").as[String] match
      case Right("plain") => j.downField("content").as[Seq[ValData]].map(Plain.apply)
      case Right("section") => j.downField("content").as[Seq[SectionData]].map(Section.apply)
      case Right(s) => Left(DecodingFailure(DecodingFailure.Reason.CustomReason(s"Error deserializing ContentData, unknown type $s"), j))
      case Left(f) => Left(f)

sealed trait ContentData