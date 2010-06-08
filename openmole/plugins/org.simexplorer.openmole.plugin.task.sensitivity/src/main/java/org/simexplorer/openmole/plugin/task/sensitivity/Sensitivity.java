/*
 *  Copyright (C) 2010 Cemagref
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
package org.simexplorer.openmole.plugin.task.sensitivity;

import java.util.ArrayList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.capsule.ExplorationTaskCapsule;
import org.openmole.core.implementation.capsule.TaskCapsule;
import org.openmole.core.implementation.mole.FixedEnvironmentStrategy;
import org.openmole.core.implementation.mole.Mole;
import org.openmole.core.implementation.plan.Factor;
import org.openmole.core.implementation.task.ExplorationTask;
import org.openmole.core.implementation.transition.AggregationTransition;
import org.openmole.core.implementation.transition.ExplorationTransition;
import org.openmole.core.implementation.transition.SingleTransition;
import org.openmole.core.model.capsule.ITaskCapsule;
import org.openmole.core.model.domain.IDomain;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.plan.IFactor;
import org.openmole.core.model.task.ITask;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public final class Sensitivity {

    private ExplorationTask askTask;
    private ITask modelTask;
    private ITask reportTask;
    private ITaskCapsule modelCapsule;
    private FixedEnvironmentStrategy modelStrategy;
    private List<IFactor> factors;

    public Sensitivity() throws UserBadDataError, InternalProcessingError {
        clear();
    }

    public final Sensitivity clear() throws UserBadDataError, InternalProcessingError {
        askTask = new ExplorationTask("ask");
        factors = new ArrayList<IFactor>();
        this.modelCapsule = new TaskCapsule();
        modelStrategy = new FixedEnvironmentStrategy();
        return this;
    }

    public Sensitivity addFactor(String name, Class type, IDomain domain) {
        this.factors.add(new Factor(name, type, domain));
        return this;
    }

    public Sensitivity setModelTask(ITask task) {
        this.modelTask = task;
        this.modelCapsule.setTask(modelTask);
        return this;
    }

    public Sensitivity setReportTask(ITask reportTask) {
        this.reportTask = reportTask;
        return this;
    }

    public Sensitivity setModelEnvironment(IEnvironment env) {
        modelStrategy.setEnvironment(modelCapsule, env);
        return this;
    }

    public IMoleExecution fast99(int samplingNumber) throws UserBadDataError, InternalProcessingError, InterruptedException {
        FastPlan fastPlan = new FastPlan(samplingNumber);
        // Ask
        askTask.setPlan(fastPlan);
        ExplorationTaskCapsule explorationCapsule = new ExplorationTaskCapsule(askTask);
        // Model
        for (IFactor factor : factors) {
            modelTask.addInput(factor.getPrototype());
            fastPlan.addFactor(factor);
        }
        modelTask.addOutput(TellTask.getModelOutputPrototype());
        // Tell
        TellTask tellTask = new TellTask("tell");
        TaskCapsule tellCapsule = new TaskCapsule(tellTask);
        // Report
        reportTask.addInput(TellTask.getAnalysisI1Prototype());
        reportTask.addInput(TellTask.getAnalysisItPrototype());
        TaskCapsule reportCapsule = new TaskCapsule(reportTask);
        // Transition
        new ExplorationTransition(explorationCapsule, modelCapsule);
        new AggregationTransition(modelCapsule, tellCapsule);
        new SingleTransition(tellCapsule, reportCapsule);
        return new Mole(explorationCapsule).createExecution(modelStrategy);
    }
}
