/*
 *  Copyright (C) 2010 Cemagref
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

package org.simexplorer.ui.ide.workflow.model;

import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.model.data.IPrototype;
import org.simexplorer.core.workflow.model.metada.Metadata;
import org.simexplorer.core.workflow.model.metada.MetadataHandler;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class PrototypeWithMetadata extends Prototype implements MetadataHandler {

    private Metadata metadata = new Metadata();

    public PrototypeWithMetadata(String name, Class type) {
        super(name, type);
    }

    public PrototypeWithMetadata(IPrototype prototype) {
        super(prototype.getName(), prototype.getType());
    }

    @Override
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    @Override
    public void setMetadata(String key, String value) {
        metadata.set(key, value);
    }

}
