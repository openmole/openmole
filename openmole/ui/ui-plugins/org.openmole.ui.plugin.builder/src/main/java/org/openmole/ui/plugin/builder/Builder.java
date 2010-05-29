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

import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.structuregenerator.PrototypeNode;
import org.openmole.core.workflow.implementation.capsule.ExplorationTaskCapsule;
import org.openmole.core.workflow.implementation.capsule.TaskCapsule;
import org.openmole.core.workflow.implementation.data.Prototype;
import org.openmole.core.workflow.implementation.mole.Mole;
import org.openmole.core.workflow.implementation.plan.Factor;
import org.openmole.core.workflow.implementation.task.ExplorationTask;
import org.openmole.core.workflow.implementation.transition.ExplorationTransition;
import org.openmole.core.workflow.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.domain.IDomain;
import org.openmole.core.workflow.model.plan.IFactor;
import org.openmole.core.workflow.model.task.IExplorationTask;
import org.openmole.core.workflow.model.task.ITask;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.plugin.domain.interval.RangeDouble;
import org.openmole.plugin.domain.interval.RangeInteger;
import org.openmole.plugin.plan.complete.CompletePlan;
import org.openmole.plugin.task.groovy.GroovyTask;
import org.openmole.plugin.task.structuregeneration.ModelStructuresGenerationTask;

import static org.openmole.ui.plugin.transitionfactory.TransitionFactory.buildChain;

public class Builder {

    public GroovyTask buildGroovyTask(String name) throws UserBadDataError, InternalProcessingError {
        return new GroovyTask(name);
    }

    public GroovyTask buildGroovyTask(String name, String code) throws UserBadDataError, InternalProcessingError {
        GroovyTask groovyTask = new GroovyTask(name);
        groovyTask.setCode(code);
        return groovyTask;
    }

    public GroovyTask buildGroovyTaskFromFile(String name, String filename) throws UserBadDataError, InternalProcessingError {
        GroovyTask groovyTask = new GroovyTask(name);
        groovyTask.setCodeFile(filename);
        return groovyTask;
    }

    public Prototype buildPrototype(String name, Class type) {
        return new Prototype(name, type);
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

        public RangeInteger buildRange(Integer min, Integer max, Integer step) {
            return new RangeInteger(min.toString(), max.toString(), step.toString());
        }

        public RangeDouble buildRange(Double min, Double max, Double step) {
            return new RangeDouble(min.toString(), max.toString(), step.toString());
        }

        public ExplorationTask buildCompletePlanTask(String name, Factor... factors) throws UserBadDataError, InternalProcessingError {
            CompletePlan plan = new CompletePlan();
            for (Factor factor : factors) {
                plan.addFactor(factor);
            }
            return new ExplorationTask(name, plan);
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

        public ModelStructuresGenerationTask buildModelStructuresGenerationTask(String name, Class<?> inputDataStructure, Class<?> outputDataStructure) throws UserBadDataError, InternalProcessingError {
            return new ModelStructuresGenerationTask(name, inputDataStructure, outputDataStructure);
        }
    }
}
