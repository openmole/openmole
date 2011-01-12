/*
 *  Copyright (C) 2011 Mathieu Leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.workflow.implementation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.openmole.commons.exception.UserBadDataError;
import scala.Tuple2;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class ContainerUI implements IContainerUI {

    private Map<Tuple2<String, Class>, IEntityUI> entities = new HashMap<Tuple2<String, Class>, IEntityUI>();

    @Override
    public void register(IEntityUI entity) {
        this.entities.put(new Tuple2<String, Class>(entity.getName(), entity.getType()), entity);
    }

    @Override
    public void removeEntity(IEntityUI entity) throws UserBadDataError {
        this.entities.remove(new Tuple2<String, Class>(entity.getName(), entity.getType()));
    }

    @Override
    public IEntityUI get(String st, Class type) throws UserBadDataError {
        Tuple2 tuple = new Tuple2<String, Class>(st, type);
        if (this.entities.containsKey(tuple)) {
            return this.entities.get(tuple);
        } else {
            throw new UserBadDataError("The entity " + st + " :: " + type + " doest not exist.");
        }
    }

    public boolean contains(IEntityUI entity){
        return this.entities.containsKey(new Tuple2<String, Class>(entity.getName(), entity.getType()));
    }
    
    @Override
    public Collection<IEntityUI> getAll() {
        return entities.values();
    }

    @Override
    public void clearAll() {
        entities.clear();
    }
}
