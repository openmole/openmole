/*
 *
 *  Copyright (c) 2010, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */

package org.openmole.ui.plugin.builder;

import org.openmole.core.implementation.data.DataSet;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.structuregenerator.PrototypeNode;
import org.openmole.core.implementation.capsule.ExplorationTaskCapsule;
import org.openmole.core.implementation.capsule.TaskCapsule;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.implementation.mole.Mole;
import org.openmole.core.implementation.plan.Factor;
import org.openmole.core.implementation.transition.ExplorationTransition;
import org.openmole.core.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.domain.IDomain;
import org.openmole.core.model.plan.IFactor;
import org.openmole.core.model.task.IExplorationTask;
import org.openmole.core.model.task.ITask;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import static org.openmole.ui.plugin.transitionfactory.TransitionFactory.buildChain;

public class Builder {

    public Prototype buildPrototype(String name, Class type) {
        return new Prototype(name, type);
    }

    public DataSet buildDataSet(IPrototype...prototypes){
        return new DataSet(prototypes);
    }

    public TaskCapsule buildTaskCapsule(ITask task) {
        return new TaskCapsule(task);
    }

    public Mole buildMole(ITask... tasks) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return new Mole(buildChain(tasks).getFirstCapsule());
    }

    public Mole buildMole(IGenericTaskCapsule taskCapsule) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return new Mole(taskCapsule);
    }

    public final ExplorationBuilder exploration = new ExplorationBuilder();

    public class ExplorationBuilder {

        public IFactor buildFactor(IPrototype prototype, IDomain domain) {
            return new Factor(prototype, domain);
        }
        
        public ExplorationTaskCapsule buildExplorationTaskCapsule(IExplorationTask task) {
            return new ExplorationTaskCapsule(task);
        }

        public TaskCapsule buildExplorationTransition(IExplorationTaskCapsule explorationCapsule, ITask exploredTask) {
            TaskCapsule exploredCapsule = new TaskCapsule(exploredTask);
            new ExplorationTransition(explorationCapsule, exploredCapsule);
            return exploredCapsule;
        }
    }
    public final StructureBuilder structure = new StructureBuilder();

    public class StructureBuilder {

        public PrototypeNode buildPrototypeNode(String name, Class type) {
            return new PrototypeNode(new Prototype(name, type));
        }

        public ComplexNode buildComplexNode(String name) {
            return new ComplexNode(name);
        }

        public ComplexNode buildComplexNode(String name, ComplexNode parent) {
            return new ComplexNode(name, parent);
        }
    }
}
