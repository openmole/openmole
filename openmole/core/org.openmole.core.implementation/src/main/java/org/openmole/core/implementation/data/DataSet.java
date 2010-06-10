/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IDataSet;
import org.openmole.core.model.data.IPrototype;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class DataSet implements IDataSet {

    final List<IData<?>> data;

    public DataSet(DataSet... datasets) {
        List<IData<?>> tmpdata = new ArrayList<IData<?>>();
        for (int i = 0; i < datasets.length; i++) {
            for (IData d : datasets[i]) {
                tmpdata.add(d);
            }
        }
        data = tmpdata;
    }

    public DataSet(List<IData<?>> data) {
        this.data = data;
    }

    public DataSet(IData<?>... data) {
        this.data = Arrays.asList(data);
    }

    public DataSet(IPrototype<?>... prototypes) {
        data = new ArrayList<IData<?>>(prototypes.length);
        for (int i = 0; i < prototypes.length; i++) {
            data.add(new Data(prototypes[i]));
        }
    }

    public DataSet(DataMod dataMod, IPrototype<?>... prototypes) {
        data = new ArrayList<IData<?>>(prototypes.length);
        for (int i = 0; i < prototypes.length; i++) {
            data.add(new Data(prototypes[i], dataMod));
        }
    }

    @Override
    public Iterator<IData<?>> iterator() {
        return data.iterator();
    }
}
