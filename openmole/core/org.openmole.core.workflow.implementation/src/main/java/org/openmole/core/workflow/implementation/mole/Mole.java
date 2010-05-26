/*
 *
 *  Copyright (c) 2007, 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free SoftwcreateExecutionare Foundation; either version 3 of
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

package org.openmole.core.workflow.implementation.mole;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.resource.LocalFileCache;
import org.openmole.core.workflow.implementation.job.Context;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.workflow.implementation.execution.local.LocalExecutionEnvironment;
import org.openmole.core.workflow.model.execution.IJobStatisticCategorizationStrategy;
import org.openmole.core.workflow.model.execution.IMoleJobGroupingStrategy;
import org.openmole.core.workflow.model.mole.IMole;
import org.openmole.core.workflow.model.mole.IEnvironmentSelectionStrategy;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.mole.IMoleExecution;
import org.openmole.core.workflow.model.resource.IResource;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.core.workflow.model.transition.ITransition;

/**
 * A Mole is a workflow with a defined start capsule. Indeed, it is a set of
 * tasks ready to be executed.
 */
public class Mole implements IMole {

    private IGenericTaskCapsule<?, ?> root;
    private Map<IGenericTaskCapsule<?, ?>, IMoleJobGroupingStrategy> groupers = new HashMap<IGenericTaskCapsule<?, ?>, IMoleJobGroupingStrategy>();
    private IJobStatisticCategorizationStrategy jobStatisticCategorizationStrategy = new CapsuleJobStatisticCategorisationStrategy();

    public Mole(IGenericTaskCapsule<?, ?> root) {
        this.root = root;
    }

    @Override
    public void run() throws UserBadDataError, InternalProcessingError, InterruptedException {
        run(new FixedEnvironmentStrategy());
    }

    @Override
    public void run(IEnvironmentSelectionStrategy strategy) throws UserBadDataError, InternalProcessingError, InterruptedException {
        IMoleExecution execution = createExecution(strategy);
        Logger.getLogger(Mole.class.getName()).fine("Begin of the RUN");
        execution.start();
        execution.waitUntilEnded();
        Logger.getLogger(Mole.class.getName()).fine("End of the RUN");
    }

    @Override
    public IMoleExecution start() throws UserBadDataError, InternalProcessingError, InterruptedException {
        IMoleExecution execution = createExecution(new FixedEnvironmentStrategy());
        execution.start();
        return execution;
    }

    @Override
    public IMoleExecution start(IEnvironmentSelectionStrategy strategy) throws UserBadDataError, InternalProcessingError, InterruptedException {
        IMoleExecution execution = createExecution(strategy);
        execution.start();
        return execution;
    }

    @Override
    public IMoleExecution createExecution() throws UserBadDataError, InternalProcessingError, InterruptedException {
        return createExecution(new FixedEnvironmentStrategy());
    }


    @Override
    public IMoleExecution createExecution(IEnvironmentSelectionStrategy strategy) throws UserBadDataError, InternalProcessingError, InterruptedException {
        LocalFileCache fileCache = new LocalFileCache();

        for (IResource resource : getAllRessources()) {
            for (File file : resource.getFiles()) {
                 fileCache.fillInLocalFileCache(file, file);
            }
        }

        IContext rootContext = new Context();

        IExecutionContext exec = new ExecutionContext(fileCache, strategy);

        return createExecution(rootContext, exec);
       
    }

    @Override
    public IMoleExecution createExecution(IContext context, IExecutionContext executionContext) throws UserBadDataError, InternalProcessingError, InterruptedException {
        return new MoleExecution(this, context, executionContext);
    }

    @Override
    public void visit(IVisitor<IGenericTaskCapsule> visitor) throws InternalProcessingError, UserBadDataError {
        Set<IGenericTaskCapsule<?, ?>> tasks = new HashSet<IGenericTaskCapsule<?, ?>>();
        Queue<IGenericTaskCapsule<?, ?>> toExplore = new LinkedList<IGenericTaskCapsule<?, ?>>();

        IGenericTaskCapsule<?,?> root = getRoot();

        if (root != null) {
            toExplore.add(root);
        }

        while (!(toExplore.isEmpty())) {
            IGenericTaskCapsule<?, ?> current = toExplore.poll();

            if (!tasks.contains(current)) {
                for (ITransition transition : current.getOutputTransitions()) {
                    toExplore.add(transition.getEnd().getCapsule());
                }
                tasks.add(current);
            }

        }

        for (IGenericTaskCapsule t : tasks) {
            visitor.action(t);
           /* IGenericTask task = t.getTask();

            if (task != null) {
                if (IMoleTask.class.isAssignableFrom(task.getClass())) {
                    IMoleTask wft = (IMoleTask) t.getTask();
                    wft.getMole().visit(visitor);
                }
            }*/
        }

    }

    @Override
    public synchronized IGenericTaskCapsule<?, ?> getRoot() {
        return root;
    }

    @Override
    public synchronized void setRoot(IGenericTaskCapsule<?, ?> root) {
        this.root = root;
    }

    @Override
    public Iterable<IResource> getAllRessources() throws InternalProcessingError, UserBadDataError {
        final Collection<IResource> ressources = new HashSet<IResource>();

        visit(new IVisitor<IGenericTaskCapsule>() {

            @Override
            public void action(IGenericTaskCapsule visited) throws InternalProcessingError, UserBadDataError {
                IGenericTask task = visited.getTask();

                if (task != null) {
                    for (IResource resource : task.getResources()) {
                        ressources.add(resource);
                    }
                }
            }
        });

        return ressources;
    }

 /*  private void firstLevelVisit(IVisitor<IGenericTaskCapsule<?, ?>> visitor) throws InternalProcessingError, UserBadDataError {
        Set<IGenericTaskCapsule<?, ?>> tasks = new HashSet<IGenericTaskCapsule<?, ?>>();
        Queue<IGenericTaskCapsule<?, ?>> toExplore = new LinkedList<IGenericTaskCapsule<?, ?>>();

        IGenericTaskCapsule<?,?> root = getRoot();
        if (root != null) {
            toExplore.add(root);
        }

        while (!(toExplore.isEmpty())) {
            IGenericTaskCapsule<?, ?> current = toExplore.poll();

            if (!tasks.contains(current)) {
                for (ITransition transition : current.getOutputTransitions()) {
                    toExplore.add(transition.getEnd().getCapsule());
                }
                tasks.add(current);
            }

        }

        for (IGenericTaskCapsule<?, ?> t : tasks) {
            visitor.action(t);
        }

    }*/

    public IMoleJobGroupingStrategy getMoleJobGroupingStrategy(IGenericTaskCapsule key) {
        return groupers.get(key);
    }

    @Override
    public void setMoleJobGroupingStrategy(IGenericTaskCapsule forCapsule, IMoleJobGroupingStrategy strategy) {
        groupers.put(forCapsule, strategy);
    }

    @Override
    public void setJobStatisticCategorizationStrategy(IJobStatisticCategorizationStrategy jobStatisticCategorizationStrategy) {
        this.jobStatisticCategorizationStrategy = jobStatisticCategorizationStrategy;
    }

    public IJobStatisticCategorizationStrategy getJobStatisticCategorizationStrategy() {
        return jobStatisticCategorizationStrategy;
    }



}
