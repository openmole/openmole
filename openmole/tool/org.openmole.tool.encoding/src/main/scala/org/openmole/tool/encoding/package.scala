package org.openmole.tool.encoding

/*
 * Copyright (C) 2025 Romain Reuillon
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

import java.util.UUID
import java.nio.ByteBuffer

object Base36:

  def uuidToString(uuid: UUID): String =
    val buffer = ByteBuffer.allocate(16)
    buffer.putLong(uuid.getMostSignificantBits)
    buffer.putLong(uuid.getLeastSignificantBits)
    val bytes = buffer.array
    val number = BigInt(1, bytes)
    number.toString(36)

  def stringToUUID(base36: String): UUID =
    val number = BigInt(base36, 36)
    val byteArray = number.toByteArray

    val bytes: Array[Byte] =
      if byteArray.length == 16 then byteArray
      else
        val padded = Array.ofDim[Byte](16)
        val offset = 16 - byteArray.length
        Array.copy(byteArray, 0, padded, offset, byteArray.length)
        padded

    val buffer = ByteBuffer.wrap(bytes)
    val high = buffer.getLong()
    val low = buffer.getLong()
    UUID(high, low)

