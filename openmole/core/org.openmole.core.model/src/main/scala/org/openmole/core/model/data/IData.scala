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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.data

/**
 *
 * {@link IData} modelizes data atomic elements of data-flows.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
trait IData[T] {


    /**
     *
     * Get the mode of the data
     *
     * @return mode of the data
     */
    def mode: IDataMode


    /**
     *
     * Get the prototype of the data.
     *
     * @return the prototype of the data
     */
    def prototype: IPrototype[T]
}
