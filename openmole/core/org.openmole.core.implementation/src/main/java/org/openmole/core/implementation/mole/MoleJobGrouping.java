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

package org.openmole.core.implementation.mole;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.execution.IGroupingStrategy;
import org.openmole.core.model.mole.IMoleJobGrouping;

/**
 *
 * @author reuillon
 */
public class MoleJobGrouping implements IMoleJobGrouping {

    static IMoleJobGrouping EMPTY_GROUPING = new MoleJobGrouping(Collections.EMPTY_MAP);
    
    final private Map<IGenericTaskCapsule<?, ?>, IGroupingStrategy> groupers;

    public MoleJobGrouping() {
        this.groupers = new HashMap<IGenericTaskCapsule<?, ?>, IGroupingStrategy>();
    }
    
    private MoleJobGrouping(Map<IGenericTaskCapsule<?, ?>, IGroupingStrategy> groupers) {
        this.groupers = groupers;
    }
    
    @Override
    public IGroupingStrategy getGroupingStrategy(IGenericTaskCapsule capsule) {
        return groupers.get(capsule);
    }

    @Override
    public void setGroupingStrategy(IGenericTaskCapsule capsule, IGroupingStrategy strategy) {
        groupers.put(capsule, strategy);
    }

    
    
}
