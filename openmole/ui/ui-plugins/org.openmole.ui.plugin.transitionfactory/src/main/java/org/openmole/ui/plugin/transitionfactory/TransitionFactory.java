/*
 *  Copyright (C) 2010 leclaire
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

package org.openmole.ui.plugin.transitionfactory;

import org.apache.commons.lang.ArrayUtils;
import org.openmole.core.implementation.capsule.ExplorationTaskCapsule;
import org.openmole.core.implementation.capsule.TaskCapsule;
import org.openmole.core.implementation.transition.AggregationTransition;
import org.openmole.core.implementation.transition.ExplorationTransition;
import org.openmole.core.implementation.transition.SingleTransition;
import org.openmole.core.model.capsule.ITaskCapsule;
import org.openmole.core.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.task.IExplorationTask;
import org.openmole.core.model.task.ITask;

/**
 *
 * @author mathieu
 */
public class TransitionFactory {

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> build(ITaskCapsule singleCapsule) {
        return new PuzzleFirstAndLast(singleCapsule, singleCapsule);
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> build(ITask singleTask) {
        return build(new TaskCapsule(singleTask));
    }
    /**
     * Creates a single transition between taskCapsules, making thus a chain. For
     * instance, if 3 capsules C1, C2, C3 are given, 2 single transitions are
     * instanciated between C1 and C2 as well as between C2 and C3.
     *
     * @param capsules, the array of all the capsules to be chained.
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildChain(ITaskCapsule... capsules) {
        int startIndex = 0;
        for (int i = 1; i < capsules.length; i++) {
            new SingleTransition(capsules[startIndex], capsules[i]);
            startIndex++;
        }
        return new PuzzleFirstAndLast(capsules[0], capsules[capsules.length - 1]);
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildChain(ITask... tasks) {
        ITaskCapsule[] capsules = new ITaskCapsule[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            capsules[i] = new TaskCapsule(tasks[i]);
        }
        return buildChain(capsules);
    }

    private static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildChain(ITaskCapsule capsule, IGenericTaskCapsule genCapsule) {
        new SingleTransition(capsule, genCapsule);
        return new PuzzleFirstAndLast(capsule, genCapsule);
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildChain(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... workflowPuzzles) {
        int length = workflowPuzzles.length;
        int startIndex = 0;
        for (int i = 1; i < length; i++) {
            new SingleTransition(workflowPuzzles[startIndex].getLastCapsule(), workflowPuzzles[i].getFirstCapsule());
            startIndex++;
        }
        return new PuzzleFirstAndLast(workflowPuzzles[0].getFirstCapsule(), workflowPuzzles[length - 1].getLastCapsule());
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildDiamond(ITaskCapsule... capsules) throws InstantiationException {
        return new PuzzleFirstAndLast(buildFork((ITaskCapsule[]) (ArrayUtils.subarray(capsules, 0, capsules.length - 1))).getFirstCapsule(),
                buildJoin((ITaskCapsule[]) (ArrayUtils.subarray(capsules, 1, capsules.length))).getLastCapsule());

    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildDiamond(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) throws InstantiationException {
        return new PuzzleFirstAndLast(buildFork((IPuzzleFirstAndLast[]) (ArrayUtils.subarray(puzzles, 0, puzzles.length - 1))).getFirstCapsule(),
                buildJoin((IPuzzleFirstAndLast[]) (ArrayUtils.subarray(puzzles, 1, puzzles.length))).getLastCapsule());
    }

    /**
     * Creates a single transition between a start capsule and several end capsules.
     * The first argument represents the starting capsule and each following ones
     * are end capsules. For instance, if a 4 capsules and C1, C2, C3 and C4 are given,
     * 3 single transitions are instanciated between C1 and C2, C1 and C3, C11 and C4.
     *
     * @param a list of capsule to be connected.
     */
    private static PuzzleFirst buildFork(ITaskCapsule... capsules) throws InstantiationException {
        if (capsules.length < 2) {
            throw new InstantiationException("Invalid number of arguments. Two at least are required.");
        } else {
            ITaskCapsule startCapsule = capsules[0];
            for (int i = 1; i < capsules.length; i++) {
                new SingleTransition(startCapsule, capsules[i]);
            }
        }
        return new PuzzleFirst(capsules[0]);
    }

    private static PuzzleFirst buildFork(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) throws InstantiationException {
        ITaskCapsule capsules[] = new ITaskCapsule[puzzles.length];
        capsules[0] = puzzles[0].getLastCapsule();
        for (int i = 1; i < puzzles.length; i++) {
            capsules[i] = puzzles[i].getFirstCapsule();
        }
        return buildFork(capsules);
    }

    /**
     * Creates a single transition between each start capsule and the end capsule.
     * For instance, if a E1 capsule and S1, S2, S3 capsules are given, 3 single
     * transitions are instanciated between S1 and E1, S2 and E1, S3 and E1.
     *
     * @param endCapsule, the end point for each transition.
     * @param startCapsules, an array containing all transition start points.
     */
    private static PuzzleLast buildJoin(ITaskCapsule... capsules) throws InstantiationException {
        if (capsules.length < 2) {
            throw new InstantiationException("Invalid number of arguments. Two at least are required.");
        } else {
            ITaskCapsule finalCapsule = capsules[capsules.length - 1];
            for (int i = 0; i < capsules.length - 1; i++) {
                System.out.println("SINGLE TRANSITION: " + capsules[i].getClass() + " and" + finalCapsule.getClass());
                new SingleTransition(capsules[i], finalCapsule);
            }
            return new PuzzleLast(finalCapsule);
        }
    }

    private static PuzzleLast buildJoin(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) throws InstantiationException {
        ITaskCapsule capsules[] = new TaskCapsule[puzzles.length];
        for (int i = 0; i < puzzles.length - 1; i++) {
            capsules[i] = puzzles[i].getLastCapsule();
        }
        capsules[puzzles.length - 1] = puzzles[puzzles.length - 1].getFirstCapsule();
        return buildJoin(capsules);
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildBranch(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) {
        buildChain(puzzles);
        return new PuzzleFirstAndLast(puzzles[0].getFirstCapsule(), puzzles[0].getLastCapsule());
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildBranch(ITaskCapsule... capsules) {
        buildChain(capsules);
        return new PuzzleFirstAndLast(capsules[0], capsules[0]);
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildExploration(IExplorationTaskCapsule exploreCapsule, IPuzzleFirstAndLast<? extends IGenericTaskCapsule, ? extends ITaskCapsule> puzzle, ITaskCapsule aggregationCapsule) {
        new ExplorationTransition(exploreCapsule, puzzle.getFirstCapsule());
        new AggregationTransition(puzzle.getLastCapsule(), aggregationCapsule);
        return new PuzzleFirstAndLast(exploreCapsule, aggregationCapsule);
    }

     public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildExploration(IExplorationTask exploreTask, IPuzzleFirstAndLast<? extends IGenericTaskCapsule, ? extends ITaskCapsule> puzzle, ITask aggregationTask) {
         return buildExploration(new ExplorationTaskCapsule(exploreTask), puzzle, new TaskCapsule(aggregationTask));
     }

     public static IPuzzleFirst<? extends ITaskCapsule> buildExploration(IExplorationTaskCapsule exploreCapsule, IPuzzleFirstAndLast<? extends IGenericTaskCapsule, ? extends ITaskCapsule> puzzle) {
        new ExplorationTransition(exploreCapsule, puzzle.getFirstCapsule());
        return new PuzzleFirst(exploreCapsule);
    }

     public static IPuzzleFirst<? extends ITaskCapsule> buildExploration(IExplorationTask exploreTask, IPuzzleFirstAndLast<? extends IGenericTaskCapsule, ? extends ITaskCapsule> puzzle) {
        ExplorationTaskCapsule etc = new ExplorationTaskCapsule(exploreTask);
        new ExplorationTransition(etc, puzzle.getFirstCapsule());
        return new PuzzleFirst(etc);
    }
}
