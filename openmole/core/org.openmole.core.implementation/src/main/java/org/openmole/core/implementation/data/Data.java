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

import org.openmole.core.model.data.DataModeMask;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IDataMode;
import org.openmole.core.model.data.IPrototype;

public class Data<T> implements IData<T> {

    IPrototype<T> prototype;
    IDataMode mode;

    public Data(IPrototype<T> prototype) {
        this(prototype, DataMode.NONE);
    }

    public Data(IPrototype<T> prototype, DataModeMask... masks) {
        this(prototype, new DataMode(masks));
    }

    public Data(IPrototype<T> prototype, IDataMode mod) {
        super();
        this.prototype = prototype;
        this.mode = mod;
    }

    public Data(String name, Class<? extends T> type) {
        this(new Prototype<T>(name, type));
    }

    public Data(String name, Class<? extends T> type, DataModeMask... masks) {
        this(new Prototype<T>(name, type), masks);
    }

    @Override
    public IPrototype<T> getPrototype() {
        return prototype;
    }

    @Override
    public IDataMode getMode() {
        return mode;
    }

}
