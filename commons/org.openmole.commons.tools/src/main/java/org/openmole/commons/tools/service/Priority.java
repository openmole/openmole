/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.commons.tools.service;

/**
 *
 * @author reuillon
 */
public enum Priority {
    HIGHEST(Integer.MAX_VALUE),
    HIGH(Integer.MAX_VALUE / 2),
    NORMAL(0),
    LOW(Integer.MIN_VALUE / 2),
    LOWEST(Integer.MIN_VALUE);

    final private Integer value;

    private Priority(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

}
