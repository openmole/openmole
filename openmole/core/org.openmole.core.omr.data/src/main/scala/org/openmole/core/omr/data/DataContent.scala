package org.openmole.core.omr.data

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

object DataContent:
  given Codec[DataContent] = Codec.AsObject.derivedConfigured

  object SectionData:
    given Codec[SectionData] = Codec.AsObject.derivedConfigured

  case class SectionData(name: Option[String], variables: Seq[ValData])

case class DataContent(section: Seq[DataContent.SectionData]) 