/*
 *
 *  Copyright (c) 2008, 2009, Cemagref
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
package org.simexplorer.ui.ide.workflow.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.openide.util.Exceptions;
import org.openmole.misc.clonning.internal.ClonningService;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.task.ExplorationTask;
import org.openmole.core.workflow.implementation.task.GenericTask;
import org.openmole.core.workflow.implementation.task.Task;
import org.openmole.core.workflow.implementation.transition.AggregationTransition;
import org.openmole.core.workflow.implementation.capsule.ExplorationTaskCapsule;
import org.openmole.core.workflow.implementation.transition.ExplorationTransition;
import org.openmole.core.workflow.implementation.transition.SingleTransition;
import org.openmole.core.workflow.implementation.capsule.TaskCapsule;
import org.openmole.core.workflow.implementation.plan.Plan;
import org.openmole.core.workflow.implementation.mole.Mole;
import org.openmole.core.workflow.implementation.data.Prototype;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.plan.IFactor;
import org.openmole.core.workflow.model.task.ITask;
import org.openmole.plugin.task.groovy.GroovyTask;

public class ExplorationApplication {

    public static final String LABEL_EXPLORATION = "Exploration";
    public static final String LABEL_INPUT_GENERATION = "Input_generation";
    public static final String LABEL_MODEL_LAUNCHER = "Model_launcher";
    public static final String LABEL_OUTPUT_PROCESSING = "Output_processing";
    public static final String LABEL_FINAL_OUTPUT_PROCESSING = "Final_processing";
    public transient static final String SYSTEM_VARIABLE_METADATA_NAME = "system";
    public transient static final String SYSTEM_VARIABLE_METADATA_VALUE = Boolean.toString(true);
    public transient static final String VARIABLE_AFTEREXPLORATION_METADATA_NAME = "afterExploration";
    public transient static final String VARIABLE_AFTEREXPLORATION_METADATA_VALUE = Boolean.toString(true);
    //public transient static final Prototype VARIABLES_PROTOTYPE = new Prototype<Context>(InformationSystem.VARIABLES_NAME, Context.class);
    private transient File savedAs = null;
    private List<IPrototype> contracts;
    private ExplorationTreeTask explorationTreeTask;
    private Prototype factorsPrototype;
    private GroovyTask finalTask;
    private TasksList treeRoot;

    // TODO How to serialize contracts ?
    /**
     * Build an exploration application with a default workflow
     * @param name
     * @throws org.simexplorer.exception.UserBadDataError
     */
    public ExplorationApplication(String name) throws UserBadDataError, InternalProcessingError {
        this.contracts = new ArrayList<IPrototype>();
        try {
            treeRoot = new TasksList(name);
            explorationTreeTask = new ExplorationTreeTask(new ExplorationTask(LABEL_EXPLORATION));

            treeRoot.add(explorationTreeTask);
            explorationTreeTask.add(new TasksList(LABEL_INPUT_GENERATION));
            explorationTreeTask.add(new TasksList(LABEL_MODEL_LAUNCHER));
            explorationTreeTask.add(new TasksList(LABEL_OUTPUT_PROCESSING));
            finalTask = new GroovyTask(LABEL_FINAL_OUTPUT_PROCESSING);
            treeRoot.add(finalTask);
        } catch (UserBadDataError ex) {
            Exceptions.printStackTrace(ex);
        } catch (InternalProcessingError ex) {
            Exceptions.printStackTrace(ex);
        }
        initSystemVariables();
    }

    public String getName() {
        return treeRoot.getName();
    }

    public boolean isSaved() {
        return savedAs != null;
    }

    public File getFileSaved() {
        return savedAs;
    }

    public void setSavedAs(File savedAs) {
        this.savedAs = savedAs;
    }

    private void initSystemVariables() {
        putContract(explorationTreeTask.getInputStructurePrototype());
        MetadataProxy.setMetadata(explorationTreeTask.getInputStructurePrototype(), SYSTEM_VARIABLE_METADATA_NAME, SYSTEM_VARIABLE_METADATA_VALUE);
        putContract(explorationTreeTask.getOutputStructurePrototype());
        MetadataProxy.setMetadata(explorationTreeTask.getOutputStructurePrototype(), SYSTEM_VARIABLE_METADATA_NAME, SYSTEM_VARIABLE_METADATA_VALUE);
        factorsPrototype = new PrototypeWithMetadata("factors", List.class);
        putContract(factorsPrototype);
        MetadataProxy.setMetadata(factorsPrototype, SYSTEM_VARIABLE_METADATA_NAME, SYSTEM_VARIABLE_METADATA_VALUE);
    }

    public TasksList getTreeRoot() {
        return treeRoot;
    }

    public ExplorationTask getExplorationTask() {
        return explorationTreeTask.getExplorationTask();
    }

    public List<IFactor<?, ?>> getFactors() {
        return explorationTreeTask.getFactors();
    }

    public IPrototype getOutputStructurePrototype() {
        return explorationTreeTask.getOutputStructurePrototype();
    }

    public ComplexNode getOutputStructure() {
        return explorationTreeTask.getOutputStructure();
    }

    public IPrototype getInputStructurePrototype() {
        return explorationTreeTask.getInputStructurePrototype();
    }

    public ComplexNode getInputStructure() {
        return explorationTreeTask.getInputStructure();
    }

    public Collection<IPrototype> getVariablesContracts() {
        return contracts;
    }

    /**
     * System variable are variables that the user cannot edit or delete in the GUI.
     * @param var
     * @return if the variable is a system variable.
     */
    public static boolean isVariableSystem(IPrototype var) {
        return SYSTEM_VARIABLE_METADATA_VALUE.equals(MetadataProxy.getMetadata(var, SYSTEM_VARIABLE_METADATA_NAME));
    }

    public static boolean isGatheredAfterExploration(IPrototype contract) {
        return VARIABLE_AFTEREXPLORATION_METADATA_VALUE.equals(MetadataProxy.getMetadata(contract, VARIABLE_AFTEREXPLORATION_METADATA_NAME));
    }

    public static void setGatheredAfterExploration(IPrototype contract, boolean afterExploration) {
        MetadataProxy.setMetadata(contract, VARIABLE_AFTEREXPLORATION_METADATA_NAME, Boolean.toString(afterExploration));
    }

    public static boolean isAfterExploration(Task task) {
        return task.getName().equals(LABEL_FINAL_OUTPUT_PROCESSING);
    }

    public void putContract(IPrototype contract, boolean afterExploration) {
        contracts.add(contract);
        setGatheredAfterExploration(contract, afterExploration);
    }

    /**
     * The metadata named {@link #VARIABLE_AFTEREXPLORATION_METADATA_NAME} is
     * assumed to be setted if the contract should continue after the exploration
     * @param contract
     */
    public void putContract(IPrototype contract) {
        contracts.add(contract);
    }

    public Boolean removeContract(IPrototype contract) {
        return contracts.remove(contract);
    }

    public Mole buildWorkflow() throws UserBadDataError {
        // sync factors in the plan method
        for (IFactor factor : getFactors()) {
            ((Plan) explorationTreeTask.getExplorationTask().getPlan()).addFactor(factor);
        }
        // add each factor as contract
        for (IFactor factor : getFactors()) {
            putContract(factor.getPrototype());
            if (isGatheredAfterExploration(factorsPrototype)) {
                setGatheredAfterExploration(factor.getPrototype(), true);
            }
        }
        // remove the fake factors list
        removeContract(factorsPrototype);
        // variables contracts
        for (IPrototype contract : contracts) {
            // applying contract in ModelStructuresGenerationTask except input and ouput variable
            if (!(contract.getName().equals(explorationTreeTask.getInputStructurePrototype().getName())) && !(contract.getName().equals(explorationTreeTask.getOutputStructurePrototype().getName()))) {
                explorationTreeTask.getModelStructuresGenerationTask().addInput(contract);
                explorationTreeTask.getModelStructuresGenerationTask().addOutput(contract);
            }
            // applying contract in other tasks
            for (GenericTask tasksList : explorationTreeTask) {
                for (GenericTask task : (TasksList) tasksList) {
                    task.addInput(contract);
                    task.addOutput(contract);
                }
            }
            // aggregation if needed
            if (isGatheredAfterExploration(contract)) {
                finalTask.addInput(contract.array());
            }
        }
        // workflow
        ExplorationTaskCapsule explorationCapsule = new ExplorationTaskCapsule(getExplorationTask());
        TaskCapsule structureCapsule = new TaskCapsule(explorationTreeTask.getModelStructuresGenerationTask());
        new ExplorationTransition(explorationCapsule, structureCapsule);
        TaskCapsule previous = structureCapsule;
        TaskCapsule current = null;
        for (GenericTask tasksList : explorationTreeTask) {
            for (GenericTask task : (TasksList) tasksList) {
                if (task instanceof Task) {
                    current = new TaskCapsule((ITask) task);
                    new SingleTransition((TaskCapsule) previous, current);
                } else {
                    Logger.getLogger(ExplorationApplication.class.getName()).warning("Generic task are not handled here. Your task was: " + task.getName());
                }
            }
        }
        new AggregationTransition(current, new TaskCapsule(finalTask));
        return new Mole(explorationCapsule);
    }

    public ExplorationApplication copy() {
        try {
            return (ExplorationApplication) new ClonningService().clone(this);
        } catch (InternalProcessingError ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }
}
