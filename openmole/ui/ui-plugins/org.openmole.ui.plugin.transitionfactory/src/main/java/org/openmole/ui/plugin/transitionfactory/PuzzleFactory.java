/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
import org.openmole.core.implementation.capsule.TaskCapsule;
import org.openmole.core.implementation.transition.AggregationTransition;
import org.openmole.core.implementation.transition.ExplorationTransition;
import org.openmole.core.implementation.transition.Transition;
import org.openmole.core.model.capsule.ITaskCapsule;
import org.openmole.core.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.plugin.transitionfactory.IPuzzleFirstAndLast;

/**
 *
 * @author mathieu
 */
public class PuzzleFactory {

    /**
     * Creates a PuzzlleFirstAndLast from an ITaskCapsule. It will be the integrated in a
     * more generic workflow as a puzzle.
     *
     * @param singleCapsule, the ITaskCapsule to be encapsuled in a IPuzzleFirstAndLast
     * @return
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> build(ITaskCapsule singleCapsule) {
        return new PuzzleFirstAndLast(singleCapsule, singleCapsule);
    }

    /**
     * Creates single transitions between taskCapsules, making thus a chain. For
     * instance, if 3 capsules C1, C2, C3 are given, 2 signle transitions are
     * instanciated between C1 and C2 as well as between C2 and C3.
     *
     * C1 - C2 - ... - Cn
     *
     * @param capsules, the array of all the capsules to be chained.
     * @return an IPuzzleFirstAndLast instance
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildChain(ITaskCapsule... capsules) {
        int startIndex = 0;
        for (int i = 1; i < capsules.length; i++) {
            new Transition(capsules[startIndex], capsules[i]);
            startIndex++;
        }
        return new PuzzleFirstAndLast(capsules[0], capsules[capsules.length - 1]);
    }

    /**
     * Creates a single transition between an ITaskCapsule and a IGenericTaskCapsule, making thus a chain.
     *
     * @param capsule, an ITaskCapsule and genCapsule an IGenericTaskCapsule
     * @return an instance of IPuzzleFirstAndLast
     */
    private static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildChain(ITaskCapsule capsule, IGenericTaskCapsule genCapsule) {
        new Transition(capsule, genCapsule);
        return new PuzzleFirstAndLast(capsule, genCapsule);
    }

    /**
     * Creates transitions between each IPuzzleFirstAndLast
     *
     * P1 - P2 - ... - Pn
     *
     * @param workflowPuzzles, an array of IPuzzleFirstAndLast
     * @return an instance of IPuzzleFirstAndLast
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildChain(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... workflowPuzzles) {
        int length = workflowPuzzles.length;
        int startIndex = 0;
        for (int i = 1; i < length; i++) {
            new Transition(workflowPuzzles[startIndex].getLastCapsule(), workflowPuzzles[i].getFirstCapsule());
            startIndex++;
        }
        return new PuzzleFirstAndLast(workflowPuzzles[0].getFirstCapsule(), workflowPuzzles[length - 1].getLastCapsule());
    }

    /**
     * Creates a Diamond structure from capsules. Building a  diamond structure of n capsules consists in
     * - creating a fork from the first capsule to the n-2 following capsules
     * - creating a join from the n-2 previously forked capsules to the final capsule
     *
     *     -  C2   -
     *  C1 -  C3   - Cn
     *     -  ...  -
     *     -  Cn-1 -
     *
     * @param capsules, the array of capsules belonging to the diamond structure
     * @return an instance of IPuzzleFirstAndLast
     * @throws InstantiationException
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildDiamond(ITaskCapsule... capsules) throws InstantiationException {
        return new PuzzleFirstAndLast(buildFork((ITaskCapsule[]) (ArrayUtils.subarray(capsules, 0, capsules.length - 1))).getFirstCapsule(),
                buildJoin((ITaskCapsule[]) (ArrayUtils.subarray(capsules, 1, capsules.length))).getLastCapsule());

    }

    /**
     * Creates a Diamond structure from puzzles. Building a  diamond structure of n IPuzzleFirstAndLast consists in
     * - creating a fork from the first puzzle to the n-2 following puzzles
     * - creating a join from the n-2 previously forked puzzles to the final puzzle
     *
     *     -  P2   -
     *  P1 -  P3   - Pn
     *     -  ...  -
     *     -  Pn-1 -
     *
     * @param puzzles, the array of puzzles belonging to the diamond structure
     * @return an instance of IPuzzleFirstAndLast
     * @throws InstantiationException
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildDiamond(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) throws InstantiationException {
        return new PuzzleFirstAndLast(buildFork((IPuzzleFirstAndLast[]) (ArrayUtils.subarray(puzzles, 0, puzzles.length - 1))).getFirstCapsule(),
                buildJoin((IPuzzleFirstAndLast[]) (ArrayUtils.subarray(puzzles, 1, puzzles.length))).getLastCapsule());
    }

    /**
     * Creates single transitions between a start capsule and n-1 end capsules.
     * The first argument represents the starting capsule and each following ones
     * are end capsules. For instance, if C1, C2, C3 and C4 are given,
     * 3 single transitions are instanciated between C1 and C2, C1 and C3, C1 and C4.
     *
     *    - C2
     * C1 - C3
     *    - C4
     *
     * @param a list of capsule to be connected.
     * @return an instance of IPuzzleFirst
     */
    private static PuzzleFirst buildFork(ITaskCapsule... capsules) throws InstantiationException {
        if (capsules.length < 2) {
            throw new InstantiationException("Invalid number of arguments. Two at least are required.");
        } else {
            ITaskCapsule startCapsule = capsules[0];
            for (int i = 1; i < capsules.length; i++) {
                new Transition(startCapsule, capsules[i]);
            }
        }
        return new PuzzleFirst(capsules[0]);
    }

    /**
     * Creates single transitions between a start puzzle and n-1 end puzzles.
     * The first argument represents the starting puzzle and each following ones
     * are end puzzles. For instance, P1, P2, P3 and P4 are given,
     * 3 single transitions are instanciated between P1 and P2, P1 and P3, P1 and P4.
     *
     *    - P2
     * P1 - P3
     *    - P4
     *
     * @param a list of puzzle to be connected. 
     * @return an instance of IPuzzleFirst
     */
    private static PuzzleFirst buildFork(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) throws InstantiationException {
        ITaskCapsule capsules[] = new ITaskCapsule[puzzles.length];
        capsules[0] = puzzles[0].getLastCapsule();
        for (int i = 1; i < puzzles.length; i++) {
            capsules[i] = puzzles[i].getFirstCapsule();
        }
        return buildFork(capsules);
    }

    /**
     * Creates single transitions between each start capsules and the end capsule.
     * For instance, if C1, C2, C3, C4 capsules are given, 3 single
     * transitions are instanciated between C1 and C4, C2 and C4, C3 and C4.
     *
     * C1   -
     * ...  - Cn
     * Cn-1 -
     *
     * @param a list of capsule to be connected. 
     */
    private static PuzzleLast buildJoin(ITaskCapsule... capsules) throws InstantiationException {
        if (capsules.length < 2) {
            throw new InstantiationException("Invalid number of arguments. Two at least are required.");
        } else {
            ITaskCapsule finalCapsule = capsules[capsules.length - 1];
            for (int i = 0; i < capsules.length - 1; i++) {
                System.out.println("SINGLE TRANSITION: " + capsules[i].getClass() + " and" + finalCapsule.getClass());
                new Transition(capsules[i], finalCapsule);
            }
            return new PuzzleLast(finalCapsule);
        }
    }

    /**
     * Preates single transitions between each start puzzles and the end puzzle.
     * For instance, if P1, P2, P3, P4 puzzles are given, 3 single
     * transitions are instanciated between P1 and P4, P2 and P4, P3 and P4.
     *
     * P1   -
     * ...  - Pn
     * Pn-1 -
     *
     * @param a list of puzzle to be connected.
     * @return an instance of IPuzzleLast
     */
    private static PuzzleLast buildJoin(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) throws InstantiationException {
        ITaskCapsule capsules[] = new TaskCapsule[puzzles.length];
        for (int i = 0; i < puzzles.length - 1; i++) {
            capsules[i] = puzzles[i].getLastCapsule();
        }
        capsules[puzzles.length - 1] = puzzles[puzzles.length - 1].getFirstCapsule();
        return buildJoin(capsules);
    }

    /**
     * Creates a branch from n capsules. Abranch consists in a chain, which is only connected
     * to the rest of the workflow by its head. It is usefull when a end capsule have to be
     * included in a workflow , that is to say a capsule, which is not expected by any other capsule
     *
     * @param capsules to be chain in a branch
     * @return an instance of IPuzzlefirstAndLast
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildBranch(ITaskCapsule... capsules) {
        buildChain(capsules);
        return new PuzzleFirstAndLast(capsules[0], capsules[0]);
    }

    /**
     * Creates a branch from n puzzles. Abranch consists in a chain, which is only connected
     * to the rest of the workflow by its head. It is usefull when a end puzzle have to be
     * included in a workflow , that is to say a puzzle, which is not expected by any other puzzle
     *
     * @param puzzles to be chain in a branch
     * @return an instance of IPuzzlefirstAndLast
     */
    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildBranch(IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule>... puzzles) {
        buildChain(puzzles);
        return new PuzzleFirstAndLast(puzzles[0].getFirstCapsule(), puzzles[0].getLastCapsule());
    }

    public static IPuzzleFirstAndLast<? extends ITaskCapsule, ? extends ITaskCapsule> buildExploration(IExplorationTaskCapsule exploreCapsule, IPuzzleFirstAndLast<? extends IGenericTaskCapsule, ? extends ITaskCapsule> puzzle, ITaskCapsule aggregationCapsule) {
        new ExplorationTransition(exploreCapsule, puzzle.getFirstCapsule());
        new AggregationTransition(puzzle.getLastCapsule(), aggregationCapsule);
        return new PuzzleFirstAndLast(exploreCapsule, aggregationCapsule);
    }
}
