/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.data

import DataModeMask._

object DataMode {
  val NONE = apply(0)

  def apply(masks: DataModeMask*): DataMode =
    DataMode(masks.map(_.value).foldLeft(0)(_ | _))

  def apply(m: Int) = new DataMode {
    val mask = m

    def is(mode: DataModeMask): Boolean = (mask & mode.value) != 0

    override def toString = {
      val toDisplay = values.flatMap { m ⇒ if (this is m) Some(m.toString) else None }
      if (toDisplay.isEmpty) "None" else toDisplay.mkString(", ")
    }
  }
}

/**
 * The data mode give meta-information about the circulation of data in the
 * mole.
 */
trait DataMode {

  /**
   * Test a data mode mask against this mode
   */
  def is(mode: DataModeMask): Boolean

  def mask: Int
}
