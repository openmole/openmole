package org.simexplorer.ui.ide.workflow.model;

import java.util.ArrayList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.structuregenerator.IStructureGenerator;
import org.openmole.core.implementation.task.ExplorationTask;
import org.openmole.core.workflow.methods.task.ModelStructuresGenerationTask;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.plan.IFactor;
import org.simexplorer.ui.ide.osgiinit.Installer;

public class ExplorationTreeTask extends TasksList {

    private ExplorationTask explorationTask;
    private ComplexNode inputStructure;
    private ComplexNode outputStructure;
    private ModelStructuresGenerationTask modelStructuresGenerationTask;
    private List<IFactor<?, ?>> factors;
    private static IPrototype inputStructurePrototype = new PrototypeWithMetadata(ModelStructuresGenerationTask.InputData.getPrototype());
    private static IPrototype outputStructurePrototype = new PrototypeWithMetadata(ModelStructuresGenerationTask.OutputData.getPrototype());

    public ExplorationTreeTask(ExplorationTask explorationTask) throws UserBadDataError, InternalProcessingError {
        super(ExplorationApplication.LABEL_EXPLORATION);
        this.explorationTask = explorationTask;
        this.inputStructure = new ComplexNode("inputStructure");
        this.outputStructure = new ComplexNode("outputStructure");
        IStructureGenerator sg = Installer.getStructureGenerator();
        modelStructuresGenerationTask = new ModelStructuresGenerationTask(
                "structureGeneration",
                sg.generateClass(inputStructure),
                sg.generateClass(outputStructure));
        factors = new ArrayList<IFactor<?, ?>>();
    }

    public ExplorationTask getExplorationTask() {
        return explorationTask;
    }

    public List<IFactor<?, ?>> getFactors() {
        return factors;
    }

    public ComplexNode getInputStructure() {
        return inputStructure;
    }

    public IPrototype getInputStructurePrototype() {
        return inputStructurePrototype;
    }

    public ModelStructuresGenerationTask getModelStructuresGenerationTask() {
        return modelStructuresGenerationTask;
    }

    public ComplexNode getOutputStructure() {
        return outputStructure;
    }

    public IPrototype getOutputStructurePrototype() {
        return outputStructurePrototype;
    }
}
