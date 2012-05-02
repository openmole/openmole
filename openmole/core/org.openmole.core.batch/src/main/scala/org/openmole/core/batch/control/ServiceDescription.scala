/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.batch.control

import java.net.URI

object ServiceDescription {
  implicit def odering = new Ordering[ServiceDescription] {
    override def compare(left: ServiceDescription, right: ServiceDescription): Int = left.toString.compareTo(right.toString)
  }
}

class ServiceDescription(val description: String) {

  def this(uri: URI) = this({ if (uri.getHost == null) "localhost" else uri.getHost } + ":" + uri.getPort)

  override def equals(other: Any): Boolean = {
    if (other == null) return false
    if (!classOf[ServiceDescription].isAssignableFrom(other.asInstanceOf[AnyRef].getClass)) return false
    description == other.asInstanceOf[ServiceDescription].description
  }
  override def hashCode = description.hashCode

  override def toString = description
}