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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class PrototypesUI {
    private Map<String, PrototypeUI> prototypes = new WeakHashMap<String, PrototypeUI>();
    private static PrototypesUI instance = null;


    public void registerPrototype(PrototypeUI p) {
        prototypes.put(p.getName(), p);
    }

    public void setPrototypes(List<PrototypeUI> protos) {

    Map<String, PrototypeUI> newprotos = new WeakHashMap<String, PrototypeUI>();
        for (PrototypeUI p : protos){
            newprotos.put(p.getName(),p);
        }
        prototypes = newprotos;
    }

    public PrototypeUI getPrototype(String st) throws UserBadDataError {
        if (prototypes.containsKey(st)) {
            return prototypes.get(st);
        } else {
            throw new UserBadDataError("The prototype " + st + " doest not exist.");
        }
    }

    public Collection<PrototypeUI> getPrototypes() {
        if (prototypes.isEmpty()) {
            prototypes.put("protoInteger", new PrototypeUI("protoInteger", BigInteger.class));
            prototypes.put("protoBigDecimal", new PrototypeUI("protoBigDecimal", BigDecimal.class));
            prototypes.put("protoFile", new PrototypeUI("protoFile", File.class));
        }
        return prototypes.values();
    }


    public static PrototypesUI getInstance() {
        if (instance == null) {
            instance = new PrototypesUI();
        }
        return instance;
    }

}
