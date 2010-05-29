/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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

package org.openmole.core.implementation.data;

import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;

public class Data<T> implements IData<T> {

    IPrototype<T> prototype;
    boolean optional;

    public Data(IPrototype<T> prototype) {
        this(prototype, false);
    }

    public Data(IPrototype<T> prototype, boolean optional) {
        super();
        this.prototype = prototype;
        this.optional = optional;
    }

    public Data(String name, Class<? extends T> type) {
        this(new Prototype<T>(name, type));
    }

    public Data(String name, Class<? extends T> type, boolean optional) {
        this(new Prototype<T>(name, type), optional);
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public IPrototype<T> getPrototype() {
        return prototype;
    }
}
