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

/**
 * This class is a collection of masks to set modalties on how a task uses
 * {@see Data}.
 */
object DataModeMask {

  /**
   * All the possible values for the data mode mask.
   */
  val values = List(Optional, Explore)

}

sealed class DataModeMask(val value: Int)

/**
 * Data is optionnal. If this data is no found at the begining of a task
 * execution no error will be raised and the task should accomodate of it.
 *
 */
case object Optional extends DataModeMask(0x0001) {
  override def toString = "optional"
}

/**
 * The value corresponding to the Data can be used by an exploration
 * transition as a exploration set. This should be set only on array.
 *
 */
case object Explore extends DataModeMask(0x0002) {
  override def toString = "explore"
}