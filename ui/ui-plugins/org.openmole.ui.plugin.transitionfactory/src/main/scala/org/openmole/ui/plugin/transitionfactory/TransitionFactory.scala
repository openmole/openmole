/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.plugin.transitionfactory

import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.transition.AggregationTransition
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.task.IExplorationTask
import org.openmole.core.model.task.ITask

object TransitionFactory {

  /**
   * Creates a PuzzlleFirstAndLast from an ICapsule. It will be the integrated in a
   * more generic workflow as a puzzle.
   *
   * @param singleCapsule, the ICapsule to be encapsuled in a IPuzzleFirstAndLast
   * @return
   */
  def puzzle[T <: ICapsule](singleCapsule: T): IPuzzleFirstAndLast = new PuzzleFirstAndLast(singleCapsule, singleCapsule)
 
  def puzzle(singleTask: ITask): IPuzzleFirstAndLast = puzzle(new Capsule(singleTask))

  
  
  
  /**
   * Creates single transitions between Capsules, making thus a chain. For
   * instance, if 3 capsules C1, C2, C3 are given, 2 signle transitions are
   * instanciated between C1 and C2 as well as between C2 and C3.
   *
   * C1 - C2 - ... - Cn
   *
   * @param capsules, the array of all the capsules to be chained.
   * @return an IPuzzleFirstAndLast instance
   */
  def chain(head: ICapsule, capsules: Array[ICapsule]): IPuzzleFirstAndLast = {
    if(!capsules.isEmpty) {
      new Transition(head, capsules(0))
      for (i <- 1 until capsules.length) {
        new Transition(capsules(i - 1), capsules(i))
      }
      new PuzzleFirstAndLast(head, capsules.last)
    } else puzzle(head)
  }
  
 
  def chain(head: ITask, capsules: Array[ITask]): IPuzzleFirstAndLast = chain(new Capsule(head), capsules.map{new Capsule(_)}.toArray[ICapsule])
   
  def chain(task: ITask, firstPuzzle: IPuzzleFirst): IPuzzleFirst = {
    val first = new Capsule(task)
    new Transition(first, firstPuzzle.first)
    new PuzzleFirst(first)
  }
    
  
  /**
   * Creates transitions between each IPuzzleFirstAndLast
   *
   * P1 - P2 - ... - Pn
   *
   * @param workflowPuzzles, an array of IPuzzleFirstAndLast
   * @return an instance of IPuzzleFirstAndLast
   */
  /*def chain[F <: ICapsule](head: IPuzzleFirstAndLast[F, ICapsule], puzzles: IPuzzleFirstAndLast[ICapsule, ICapsule]*): IPuzzleFirstAndLast[F,ICapsule] = {
    if(!puzzles.isEmpty) {
      new Transition(head.lastCapsule, puzzles(0).firstCapsule)
      for (i <- 1 until puzzles.length) new Transition(puzzles(i - 1).lastCapsule, puzzles(i).firstCapsule)
      new PuzzleFirstAndLast(head.firstCapsule, puzzles.last.lastCapsule)
    } else head
  }*/

  def chain(head: IPuzzleFirstAndLast, last: IPuzzleFirstAndLast): IPuzzleFirstAndLast = {
    new Transition(head.last, last.first)
    new PuzzleFirstAndLast(head.first, last.last)
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
  def diamond(head: ICapsule, last: ICapsule, capsules: Array[ICapsule]): IPuzzleFirstAndLast = 
    new PuzzleFirstAndLast(fork(head, capsules).first, join(last, capsules).last)
  

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
  def diamond(head: IPuzzleFirstAndLast, last: IPuzzleFirstAndLast, puzzles: Array[IPuzzleFirstAndLast]): IPuzzleFirstAndLast =
    new PuzzleFirstAndLast(fork(head, puzzles).first,join(last, puzzles).last)
  

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
  private def fork(head: ICapsule, capsules: Array[ICapsule]): PuzzleFirst = {
    for (capsule <- capsules) new Transition(head, capsule)
    return new PuzzleFirst(head)
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
  private def fork(head: IPuzzleFirstAndLast, puzzles: Array[IPuzzleFirstAndLast]): IPuzzleFirst = 
    fork(head.last, puzzles.map{ _.first })
    

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
  private def join(last: ICapsule, capsules: Array[ICapsule]): IPuzzleLast = {
    for (capsule <- capsules) new Transition(capsule, last)
    return new PuzzleLast(last)
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
  private def join(last: IPuzzleFirstAndLast, puzzles: Array[IPuzzleFirstAndLast]): IPuzzleLast = {
    return join(last.first, puzzles.map{ _.last })
  }

  /**
   * Creates a branch from n capsules. Abranch consists in a chain, which is only connected
   * to the rest of the workflow by its head. It is usefull when a end capsule have to be
   * included in a workflow , that is to say a capsule, which is not expected by any other capsule
   *
   * @param capsules to be chain in a branch
   * @return an instance of IPuzzlefirstAndLast
   */
  def branch(head: ICapsule, capsules: ICapsule): IPuzzleFirstAndLast = {
    chain(head, Array(capsules))
    return new PuzzleFirstAndLast(head, head)
  }

  /**
   * Creates a branch from n puzzles. Abranch consists in a chain, which is only connected
   * to the rest of the workflow by its head. It is usefull when a end puzzle have to be
   * included in a workflow , that is to say a puzzle, which is not expected by any other puzzle
   *
   * @param puzzles to be chain in a branch
   * @return an instance of IPuzzlefirstAndLast
   */
  def branch(head: IPuzzleFirstAndLast, puzzles: IPuzzleFirstAndLast): IPuzzleFirstAndLast = {
    chain(head, puzzles)
    return new PuzzleFirstAndLast(head.first, head.first)
  }

  def exploration(exploreCapsule: ICapsule, puzzle: IPuzzleFirstAndLast, aggregationCapsule: ICapsule): IPuzzleFirstAndLast = {
    new Transition(exploreCapsule, puzzle.first)
    new AggregationTransition(puzzle.last, aggregationCapsule)
    return new PuzzleFirstAndLast(exploreCapsule, aggregationCapsule)
  }
  
  def exploration(exploreTask: IExplorationTask, puzzle: IPuzzleFirstAndLast, aggregationTask: ITask): IPuzzleFirstAndLast = 
    exploration(new Capsule(exploreTask), puzzle, new Capsule(aggregationTask))
 
  def exploration(exploreCapsule: ICapsule, puzzle: IPuzzleFirstAndLast): IPuzzleFirstAndLast = {
    new Transition(exploreCapsule, puzzle.first)
    new PuzzleFirstAndLast(exploreCapsule, puzzle.last)
  }

  def exploration(exploreTask: IExplorationTask, puzzle: IPuzzleFirstAndLast): IPuzzleFirstAndLast = {
    val etc = new Capsule(exploreTask)
    new Transition(etc, puzzle.first)
    return new PuzzleFirstAndLast(etc, puzzle.last)
  }
}
