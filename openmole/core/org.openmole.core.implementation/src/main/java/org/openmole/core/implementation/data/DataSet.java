/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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
package org.openmole.core.implementation.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IDataSet;
import org.openmole.core.model.data.IPrototype;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class DataSet implements IDataSet {

    final Map<String, IData<?>> data;

    public DataSet(IDataSet... datasets) {
        Map<String, IData<?>> data = new TreeMap<String, IData<?>>();

        for (int i = 0; i < datasets.length; i++) {
            for (IData d : datasets[i]) {
                data.put(d.getPrototype().getName(), d);
            }
        }
        this.data = Collections.unmodifiableMap(data);
    }

    public DataSet(List<IData<?>> dataList) {
        this.data = initData(dataList);
    }

    public DataSet(IData<?>... data) {
        this(Arrays.asList(data));
    }

    public DataSet(IPrototype<?>... prototypes) {
        List<IData<?>> data = new ArrayList<IData<?>>(prototypes.length);
        for (int i = 0; i < prototypes.length; i++) {
            data.add(new Data(prototypes[i]));
        }
        this.data = initData(data);
    }

    public DataSet(DataMode dataMod, IPrototype<?>... prototypes) {
        List<IData<?>> data = new ArrayList<IData<?>>(prototypes.length);
        for (int i = 0; i < prototypes.length; i++) {
            data.add(new Data(prototypes[i], dataMod));
        }
        this.data = initData(data);
    }

    public DataSet(Iterable<IData> dataSet, IPrototype<?>... prototypes) {
        List<IData<?>> data = new ArrayList<IData<?>>(prototypes.length);
        for (IData d : dataSet) {
            data.add(d);
        }

        for (int i = 0; i < prototypes.length; i++) {
            data.add(new Data(prototypes[i]));
        }
        this.data = initData(data);
    }

    public DataSet(Iterable<IData> dataSet, DataMode dataMod, IPrototype<?>... prototypes) {
        List<IData<?>> data = new ArrayList<IData<?>>(prototypes.length);
        for (IData d : dataSet) {
            data.add(d);
        }

        for (int i = 0; i < prototypes.length; i++) {
            data.add(new Data(prototypes[i], dataMod));
        }
        this.data = initData(data);
    }

    public DataSet(Iterable<IData> dataSet, IData... inData) {
        List<IData<?>> data = new ArrayList<IData<?>>(inData.length);
        for (IData d : dataSet) {
            data.add(d);
        }

        for (IData d : inData) {
            data.add(d);
        }
        this.data = initData(data);
    }


    private Map<String, IData<?>> initData(List<IData<?>> dataList) {
        Map<String, IData<?>> data = new TreeMap<String, IData<?>>();

        for (IData d : dataList) {
            data.put(d.getPrototype().getName(), d);
        }

        return  Collections.unmodifiableMap(data);
    }


    @Override
    public Iterator<IData<?>> iterator() {
        return data.values().iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public IData<?> getData(String name) {
        return data.get(name);
    }
}
