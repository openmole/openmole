/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.core.model.data;


/**
 *
 * The parameter is a variable that is injected in the data flow durring the
 * workflow execution
 *
 * @author reuillon
 */
public interface IParameter<T> {

    /**
     *
     * Get the variable which is injected.
     *
     * @return the variable
     */
    IVariable<T> getVariable();

    /**
     *
     * Get if an existing value in the context should be overriden.
     *
     * @return true if an existing value should be overriden
     */
    boolean getOverride();
}
