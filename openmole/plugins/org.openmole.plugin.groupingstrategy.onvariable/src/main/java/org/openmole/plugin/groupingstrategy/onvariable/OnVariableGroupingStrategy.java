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

package org.openmole.plugin.groupingstrategy.onvariable;

import java.util.LinkedList;
import java.util.List;
import org.openmole.core.implementation.mole.MoleJobGroup;
import org.openmole.core.model.mole.IMoleJobGroup;
import org.openmole.core.model.mole.IGroupingStrategy;
import org.openmole.core.model.data.IContext;
import org.openmole.core.model.data.IPrototype;
import scala.Option;

/**
 *
 * @author reuillon
 */
public class OnVariableGroupingStrategy implements IGroupingStrategy {

    IPrototype prototypes[];

    public OnVariableGroupingStrategy(IPrototype... prototypes) {
        this.prototypes = prototypes;
    }

    @Override
    public IMoleJobGroup group(IContext context) {
        List vals = new LinkedList();

        for(IPrototype prototype : prototypes) {
            Option<Object> val = context.value(prototype);

            if(val.isDefined()) {
                vals.add(val.get());
            }
        }

        return new MoleJobGroup(vals.toArray());
    }
    
}
