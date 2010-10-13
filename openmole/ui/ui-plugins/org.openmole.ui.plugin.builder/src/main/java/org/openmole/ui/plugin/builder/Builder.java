/*
 *
 *  Copyright (c) 2010, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
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
import org.openmole.core.implementation.transition.ExplorationTransition;
import org.openmole.core.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.domain.IDomain;
import org.openmole.core.model.task.IExplorationTask;
import org.openmole.core.model.task.ITask;
import org.openmole.core.implementation.task.MoleTask;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.task.ExplorationTask;
import org.openmole.ui.plugin.transitionfactory.IPuzzleFirstAndLast;
import static org.openmole.ui.plugin.transitionfactory.TransitionFactory.*;
import org.openmole.core.implementation.task.InputToGlobalTask;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IDataSet;
import org.openmole.ui.plugin.transitionfactory.PuzzleFirstAndLast;
import org.openmole.core.implementation.data.Util;
import org.openmole.core.implementation.data.Variable;
import org.openmole.core.implementation.mole.FixedEnvironmentSelection;
import org.openmole.core.model.mole.IEnvironmentSelection;
import org.openmole.core.model.mole.IMole;
import org.openmole.core.implementation.mole.MoleExecution;
import org.openmole.core.implementation.sampler.Factor;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.sampler.IFactor;
import org.openmole.core.model.sampler.ISampler;

/**
 *
 * Builder is a class offering factories to build complex OpenMOLE objects.
 *
 * @author nicolas.dumoulin@openmole.org
 */
public class Builder {

    /**
     * Builds an OpenMOLE prototype. A prototype is composed of a name and a type.
     *
     * @param name, the name of the protoype,
     * @param type, the class name of the type.
     * @return an instance of Prototype.
     */
    public IPrototype buildPrototype(String name, Class type) {
        return new Prototype(name, type);
    }

    /**
     * Builds an OpenMOLE variable.
     * @param name
     * @param value
     * @return an instance of Variable.
     */
    public IVariable buildVariable(String name, Object value) {
        return new Variable(name, value);
    }

    /**
     * Builds a dataSet, which is a collection of prototypes.
     *
     * @param prototypes, the prototypes to be grouped.
     * @return a DataSet
     */
    public IDataSet buildDataSet(IPrototype... prototypes) {
        return new DataSet(prototypes);
    }

    /**
     * Builds a dataSet, from a collection of dataset. In other words, it composes
     * datasets.
     *
     * @param dataSets, the dataSet to be composed.
     * @return the composed dataSet.
     */
    public IDataSet buildDataSet(DataSet... dataSets) {
        return new DataSet(dataSets);
    }

    /**
     * Builds a TaskCapsule object.
     *
     * @param task, the task to be encapsulated
     * @return an instance of TaskCapsule
     */
    public TaskCapsule buildTaskCapsule(ITask task) {
        return new TaskCapsule(task);
    }

    /**
     * Builds a MoleTask containing an exploration. The output of this task are the
     * the puzzle output as arrays.
     *
     * @param taskName, the name of the task,
     * @param explo, the exploration task,
     * @param puzzle, the puzzle.
     * @return a instance of MoleTask
     * @throws InternalProcessingError
     * @throws UserBadDataError
     * @throws InterruptedException
     */
    public MoleTask buildExplorationMoleTask(String taskName,
            IExplorationTask explo,
            PuzzleFirstAndLast puzzle) throws InternalProcessingError, UserBadDataError, InterruptedException {

        // the final task making possible the retrieving of output
        InputToGlobalTask inputToGlobalTask = new InputToGlobalTask(taskName + "InputToGlobalTask");
        for (IData data : puzzle.getLastCapsule().getTask().getOutput()) {
            if (!data.getMode().isSystem()) {
                inputToGlobalTask.addInput(Util.toArray(data));
            }
        }

        // builds a mole containing a exploration, a puzzle, and an aggregation on the inputToGlobalTask
        IMole mole = buildMole(buildExploration(explo, puzzle, inputToGlobalTask).getFirstCapsule());
        MoleTask moleTask = new MoleTask(taskName, mole);

        // sets output available as an array
        for (IData data : puzzle.getLastCapsule().getTask().getOutput()) {
            if (!data.getMode().isSystem()) {
                moleTask.addOutput(Util.toArray(data));
            }
        }
        return moleTask;
    }

    /**
     * Builds a generic MOLE Task, that is to say without any exploration.
     *
     * @param taskName, the task name,
     * @param puzzle, the puzzle to be executed in the Mole task.
     * @return a instance of MoleTask
     * @throws InternalProcessingError
     * @throws UserBadDataError
     * @throws InterruptedException
     */
    public MoleTask buildMoleTask(String taskName,
            PuzzleFirstAndLast puzzle) throws InternalProcessingError, UserBadDataError, InterruptedException {

        InputToGlobalTask inputToGlobalTask = new InputToGlobalTask(taskName + "InputToGlobalTask");

        for (IData data : puzzle.getLastCapsule().getTask().getOutput()) {
            inputToGlobalTask.addInput(data);
        }

        IMole mole = buildMole(buildChain(puzzle, build(inputToGlobalTask)).getFirstCapsule());
        MoleTask moleTask = new MoleTask(taskName, mole);

        for (IData data : puzzle.getLastCapsule().getTask().getOutput()) {
            moleTask.addOutput(data);
        }
        return moleTask;
    }

    /**
     * Builds a Mole.
     *
     * @param tasks, a list of tasks to be chained inside the Mole. Task capsules
     * are previously generated.
     * @return an instance of Mole.
     * @throws UserBadDataError
     * @throws InternalProcessingError
     * @throws InterruptedException
     */
    public IMole buildMole(ITask... tasks) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return new Mole(buildChain(tasks).getFirstCapsule());
    }

    /**
     * Builds a Mole.
     *
     * @param taskCapsule, a list of task capsules to be chained inside the Mole.
     * @return an instance of Mole.
     * @throws UserBadDataError
     * @throws InternalProcessingError
     * @throws InterruptedException
     */
    public IMole buildMole(IGenericTaskCapsule taskCapsule) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return new Mole(taskCapsule);
    }

    /**
     * Builds a Mole.
     *
     * @param puzzle, the puzzle to be executed inside the Mole.
     * @return an instance of Mole.
     * @throws UserBadDataError
     * @throws InternalProcessingError
     * @throws InterruptedException
     */
    public IMole buildMole(IPuzzleFirstAndLast puzzle) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return buildMole(puzzle.getFirstCapsule());
    }

    /**
     * Builds a Mole execution, that is to say a Mole ready to be run.
     *
     * @param tasks, a list of tasks to be chained inside the Mole.
     * @return an instance of MoleExecution
     * @throws UserBadDataError
     * @throws InternalProcessingError
     * @throws InterruptedException
     */
    public IMoleExecution buildMoleExecution(ITask... tasks) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return buildMoleExecution(buildMole(tasks));
    }

    /**
     * Builds a Mole execution, that is to say a Mole ready to be run.
     *
     * @param taskCapsule, a list of task capsules to be chained inside the Mole.
     * @return an instance of MoleExecution
     * @throws UserBadDataError
     * @throws InternalProcessingError
     * @throws InterruptedException
     */
    public IMoleExecution buildMoleExecution(IGenericTaskCapsule taskCapsule) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return buildMoleExecution(buildMole(taskCapsule));
    }

    /**
     * Builds a Mole execution, that is to say a Mole ready to be run.
     *
     * @param puzzle,  the puzzle to be executed inside the Mole.
     * @return an instance of MoleExecution
     * @throws UserBadDataError
     * @throws InternalProcessingError
     * @throws InterruptedException
     */
    public IMoleExecution buildMoleExecution(IPuzzleFirstAndLast puzzle) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return buildMoleExecution(buildMole(puzzle));
    }

    /**
     * Builds a Mole execution, that is to say a Mole ready to be run.
     *
     * @param mole, the mole to be executed.
     * @return an instance of MoleExecution.
     * @throws InternalProcessingError
     * @throws UserBadDataError
     */
    public IMoleExecution buildMoleExecution(IMole mole) throws InternalProcessingError, UserBadDataError {
        return new MoleExecution(mole);
    }

    /**
     * Builds a Mole execution, with a specific environment strategy.
     *
     * @param mole, the puzzle to be executed inside the Mole.
     * @return an instance of MoleExecution.
     * @throws InternalProcessingError
     * @throws UserBadDataError
     */
    public IMoleExecution buildMoleExecution(IPuzzleFirstAndLast puzzle,
            IEnvironmentSelection strategy) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return new MoleExecution(buildMole(puzzle), strategy);
    }

    /**
     * Builds an environment selection object.
     * @return an instance of FixedEnvironmentSelection
     * @throws InternalProcessingError
     */
    public FixedEnvironmentSelection buildFixedEnvironmentSelection() throws InternalProcessingError {
        return new FixedEnvironmentSelection();
    }
    public final ExplorationBuilder exploration = new ExplorationBuilder();

    public class ExplorationBuilder {

        /**
         * Builds a Factor according to a prototype
         *
         * @param prototype, the prototype to be
         * @param domain, to domain on which making the exploration
         * @return an instance of Factor
         */
        public IFactor buildFactor(IPrototype prototype, IDomain domain) {
            return new Factor(prototype, domain);
        }

        /**
         * Builds an exploration task, according to a Design of Experiment.
         *
         * @param name, the name of the task,
         * @param sampler, the sampler to be explored.
         * @return an instance of ExplorationTask
         * @throws UserBadDataError
         * @throws InternalProcessingError
         */
        public IExplorationTask buildExplorationTask(String name, ISampler sampler) throws UserBadDataError, InternalProcessingError {
            return new ExplorationTask(name, sampler);
        }

        /**
         * Builds an exploration task, according to a Design of Experiment and input
         * prototypes.
         *
         * @param name, the name of the task,
         * @param sampler, the sampler to be explored.
         * @param input, a set of prototypes to be set as input of the task
         * @return an instance of ExplorationTask
         * @throws UserBadDataError
         * @throws InternalProcessingError
         */
        public IExplorationTask buildExplorationTask(String name,
                ISampler sampler,
                IDataSet input) throws UserBadDataError, InternalProcessingError {
            ExplorationTask explo = new ExplorationTask(name, sampler);
            explo.addInput(input);
            return explo;
        }

        /**
         * Builds an ExplorationTaskCapsule
         *
         * @param exporationTask, the exploration task to be encapsulated.
         * @return an instace of ExplorationTaskCapsule
         */
        public ExplorationTaskCapsule buildExplorationTaskCapsule(IExplorationTask exporationTask) {
            return new ExplorationTaskCapsule(exporationTask);
        }

        /**
         * Builds an transitin exploration from a exploration task capsule and a
         * task to be explored.
         *
         * @param explorationCapsule, the exploration task capsule.
         * @param exploredTask, the task to be explored.
         * @return an instance of TaskCapsule
         */
        public TaskCapsule buildExplorationTransition(IExplorationTaskCapsule explorationCapsule, ITask exploredTask) {
            TaskCapsule exploredCapsule = new TaskCapsule(exploredTask);
            new ExplorationTransition(explorationCapsule, exploredCapsule);
            return exploredCapsule;
        }
    }
    public final StructureBuilder structure = new StructureBuilder();

    public class StructureBuilder {

        /**
         * Builds a prototype node.
         *
         * @param name, the name of the node,
         * @param type, the type of tho node.
         * @return an instance of PrototypeNode
         */
        public PrototypeNode buildPrototypeNode(String name, Class type) {
            return new PrototypeNode(new Prototype(name, type));
        }

        /**
         *  Builds a complex prototype node.
         *
         * @param name, the name of the prototype.
         * @return an instance of ComplexNode
         */
        public ComplexNode buildComplexNode(String name) {
            return new ComplexNode(name);
        }

        /**
         * Builds a complex prototype node.
         *
         * @param name, the name of the prototype.
         * @param parent 
         * @return
         */
        public ComplexNode buildComplexNode(String name, ComplexNode parent) {
            return new ComplexNode(name, parent);
        }
    }
}
