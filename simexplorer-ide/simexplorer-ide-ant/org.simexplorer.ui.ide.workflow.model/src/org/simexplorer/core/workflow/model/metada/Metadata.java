/*
 *  Copyright (C) 2010 Cemagref
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

package org.simexplorer.core.workflow.model.metada;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class Metadata implements Iterable<Entry<String, String>> {

    private Map<String, String> map;

    public Metadata() {
        map = new HashMap<String, String>();
    }

    public Metadata(HashMap<String, String> map) {
        this();
        this.map = map;
    }

    public String get(String data) {
        return map.get(data);
    }

    public void set(String key, String value) {
        this.map.put(key, value);
    }

    public Set<Entry<String, String>> entrySet() {
        return map.entrySet();
    }

    public String[] keys() {
        return map.keySet().toArray(new String[]{});
    }

    public int size() {
        return map.size();
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        return map.entrySet().iterator();
    }
    @Override
    public String toString() {
        StringBuilder st=new StringBuilder();
        for (Entry<String, String> entry:map.entrySet()){
            st.append(entry.getKey()+" : ");
            st.append(entry.getValue()+":\n");
        }
        return st.toString();
    }
}
