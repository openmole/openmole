/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.tools;

import org.openmole.core.model.tools.ILevelComputing;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.core.model.transition.IAggregationTransition;
import org.openmole.core.model.transition.IExplorationTransition;
import org.openmole.core.model.transition.IGenericTransition;
import org.openmole.core.model.transition.ISlot;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.mole.IMole;
import org.openmole.core.model.mole.IMoleExecution;

/**
 *
 * @author reuillon
 */
public class LevelComputing implements ILevelComputing {

    static Map<IMoleExecution, LevelComputing> levelComputings = new WeakHashMap<IMoleExecution, LevelComputing>();


    static synchronized public LevelComputing getLevelComputing(IMoleExecution moleExecution) {
        LevelComputing ret = levelComputings.get(moleExecution);
        if(ret == null) {
            ret = new LevelComputing(moleExecution.getMole());
            levelComputings.put(moleExecution, ret);
        }
        return ret;
    }

    IMole workflow;
    private transient Map<IGenericTaskCapsule<?, ?>, Integer> levelCache = new HashMap<IGenericTaskCapsule<?, ?>, Integer>();

    private LevelComputing(IMole workflow) {
        this.workflow = workflow;
    }

    //TODO derecurisivate
    @Override
    public int getLevel(IGenericTaskCapsule<?, ?> capsule) {

        Integer cachedLevel = levelCache.get(capsule);
        if (cachedLevel != null) {
            return cachedLevel;
        }

        int level = getLevel(capsule, workflow, new HashSet<IGenericTaskCapsule<?, ?>>());

        if (level == Integer.MAX_VALUE) {
            Logger.getLogger(LevelComputing.class.getName()).log(Level.SEVERE, "Bug: Error in level computing level could not be equal to MAXVALUE.");
        }

        levelCache.put(capsule, level);
        return level;

    }

    public int getLevel(IGenericTaskCapsule<?, ?> capsule, IMole workflow, Set<IGenericTaskCapsule<?, ?>> allreadySeen) {
        if (allreadySeen.contains(capsule)) {
            return Integer.MAX_VALUE;
        } else {
            allreadySeen.add(capsule);
        }

        if (capsule.equals(workflow.getRoot())) {
            return 1;
        }

        int level = Integer.MAX_VALUE;

        for (ISlot group : capsule.getIntputTransitionsSlots()) {
            for (IGenericTransition t : group.getTransitions()) {
                int inLevel = getLevel(t.getStart(), workflow, allreadySeen);

                if (IExplorationTransition.class.isAssignableFrom(t.getClass())) {
                    inLevel++;
                } else if (IAggregationTransition.class.isAssignableFrom(t.getClass())) {
                    inLevel--;
                }

                if (inLevel < level) {
                    level = inLevel;
                }
            }
        }


        return level;

    }
}
