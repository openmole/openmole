/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.data


object DataModeMask {

  /**
   *
   * Data is optionnal
   *
   */
  val OPTIONAL = new DataModeMask(0x0001)

  /**
   *
   * State that the data value of a variable will not be modified
   *
   */
  val MUTABLE = new DataModeMask(0x0002)

  /**
   *
   * State that the data value of a variable that is used for system level information
   * as oposed to buisiness level informations
   *
   */
  val SYSTEM = new DataModeMask(0x0004)
}

class DataModeMask(val value: Int)