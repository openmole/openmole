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
package org.openmole.core.implementation.mole;

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
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.model.mole.IMole;
import org.openmole.core.model.mole.IEnvironmentSelection;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.core.model.transition.ITransition;

/**
 * A Mole is a workflow with a defined start capsule. Indeed, it is a set of
 * tasks ready to be executed.
 */
public class Mole implements IMole {

    private IGenericTaskCapsule<?, ?> root;
 
    public Mole(IGenericTaskCapsule<?, ?> root) {
        this.root = root;
    }

  /*  @Override
    public void run() throws UserBadDataError, InternalProcessingError, InterruptedException {
        run(new FixedEnvironmentSelection());
    }

    @Override
    public void run(IEnvironmentSelection strategy) throws UserBadDataError, InternalProcessingError, InterruptedException {
        IMoleExecution execution = new MoleExecution(this, strategy);
        Logger.getLogger(Mole.class.getName()).fine("Begin of the RUN");
        execution.start();
        execution.waitUntilEnded();
        Logger.getLogger(Mole.class.getName()).fine("End of the RUN");
    }

    @Override
    public IMoleExecution start() throws UserBadDataError, InternalProcessingError, InterruptedException {
        IMoleExecution execution = new MoleExecution(this, new FixedEnvironmentSelection());
        execution.start();
        return execution;
    }

    @Override
    public IMoleExecution start(IEnvironmentSelection strategy) throws UserBadDataError, InternalProcessingError, InterruptedException {
        IMoleExecution execution = new MoleExecution(this, strategy);
        execution.start();
        return execution;
    }*/

    @Override
    public void visit(IVisitor<IGenericTaskCapsule> visitor) throws InternalProcessingError, UserBadDataError {
        Set<IGenericTaskCapsule<?, ?>> tasks = new HashSet<IGenericTaskCapsule<?, ?>>();
        Queue<IGenericTaskCapsule<?, ?>> toExplore = new LinkedList<IGenericTaskCapsule<?, ?>>();

        IGenericTaskCapsule<?, ?> root = getRoot();

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
    public Iterable<IGenericTask> getAllTasks() throws InternalProcessingError, UserBadDataError {
        final Collection<IGenericTask> tasks = new HashSet<IGenericTask>();

        visit(new IVisitor<IGenericTaskCapsule>() {

            @Override
            public void action(IGenericTaskCapsule visited) throws InternalProcessingError, UserBadDataError {
                IGenericTask task = visited.getTask();

                if (task != null) {
                    tasks.add(task);

                }
            }
        });

        return tasks;
    }

    @Override
    public Iterable<IGenericTaskCapsule> getAllTaskCapsules() throws InternalProcessingError, UserBadDataError {
        final Collection<IGenericTaskCapsule> capsules = new HashSet<IGenericTaskCapsule>();

        visit(new IVisitor<IGenericTaskCapsule>() {

            @Override
            public void action(IGenericTaskCapsule visited) throws InternalProcessingError, UserBadDataError {
                capsules.add(visited);
            }
        });

        return capsules;
    }
}
