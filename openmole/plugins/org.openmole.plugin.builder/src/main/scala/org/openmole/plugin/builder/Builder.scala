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

package org.openmole.plugin.builder

/**
 *
 * Builder is a class offering factories to build complex OpenMOLE objects.
 *
 * @author nicolas.dumoulin@openmole.org
 */

import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.mole.StrainerCapsule
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.mole.FixedEnvironmentSelection
import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.puzzle.PuzzleFirst
import org.openmole.core.implementation.puzzle.PuzzleFirstAndLast
import org.openmole.core.implementation.puzzle.PuzzleLast
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.task.MoleTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.implementation.transition.AggregationTransition
import org.openmole.core.implementation.transition.Slot
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.IPuzzleFirst
import org.openmole.core.model.IPuzzleFirstAndLast
import org.openmole.core.model.IPuzzleLast
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.mole.IEnvironmentSelection
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.sampling.IFactor
import org.openmole.core.model.sampling.ISampling
import org.openmole.core.model.task.IExplorationTask
import org.openmole.core.model.task.ITask
import org.openmole.core.model.task.IMoleTask
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.ITransition

object Builder {

  /**
   * Builds an OpenMOLE prototype. A prototype is composed of a name and a type.
   *
   * @param name, the name of the protoype,
   * @param type, the class name of the type.
   * @return an instance of Prototype.
   */
  def prototype[T](name: String, `type`: Class[T]): IPrototype[T] =  new Prototype[T](name, `type`)
    

  /**
   * Builds an OpenMOLE variable.
   * @param name
   * @param value
   * @return an instance of Variable.
   */
  def variable[T](name: String, value: T): IVariable[T] = new Variable[T](name, value)
    

  /**
   * Builds a dataSet, which is a collection of prototypes.
   *
   * @param prototypes, the prototypes to be grouped.
   * @return a DataSet
   */
  def dataSet(head: IPrototype[_], prototypes: Array[IPrototype[_]]): IDataSet = new DataSet(head, prototypes)
    

  /**
   * Builds a dataSet, from a collection of dataset. In other words, it composes
   * datasets.
   *
   * @param dataSets, the dataSet to be composed.
   * @return the composed dataSet.
   */
  def dataSet(head: IDataSet, dataSets: Array[IDataSet]): IDataSet = new DataSet(head, dataSets)
    

  /**
   * Builds a Capsule object.
   *
   * @param task, the task to be encapsulated
   * @return an instance of Capsule
   */
  def capsule(task: ITask): ICapsule = new Capsule(task)

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
  def moleTask(taskName: String, puzzle: IPuzzleFirstAndLast) = 
    new MoleTask(taskName, new Mole(puzzle.first), puzzle.last)


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
  def mole(head: ITask, tasks: Array[ITask]): IMole = new Mole(chain(head, tasks).first)
    
  /**
   * Builds a Mole.
   *
   * @param Capsule, a list of task capsules to be chained inside the Mole.
   * @return an instance of Mole.
   * @throws UserBadDataError
   * @throws InternalProcessingError
   * @throws InterruptedException
   */
  def mole(head: ICapsule, capsules: Array[ICapsule]): IMole = new Mole(chain(head, capsules).first)
  def mole(capsule: ICapsule): IMole = new Mole(capsule)

  /**
   * Builds a Mole.
   *
   * @param puzzle, the puzzle to be executed inside the Mole.
   * @return an instance of Mole.
   * @throws UserBadDataError
   * @throws InternalProcessingError
   * @throws InterruptedException
   */
  def mole(puzzle: IPuzzleFirstAndLast): IMole = new Mole(puzzle.first)
  /**
   * Builds a Mole execution, that is to say a Mole ready to be run.
   *
   * @param tasks, a list of tasks to be chained inside the Mole.
   * @return an instance of MoleExecution
   * @throws UserBadDataError
   * @throws InternalProcessingError
   * @throws InterruptedException
   */
  def moleExecution(head: ITask, tasks: Array[ITask]): IMoleExecution = new MoleExecution(mole(head, tasks))
    

  /**
   * Builds a Mole execution, that is to say a Mole ready to be run.
   *
   * @param Capsule, a list of task capsules to be chained inside the Mole.
   * @return an instance of MoleExecution
   * @throws UserBadDataError
   * @throws InternalProcessingError
   * @throws InterruptedException
   */
  def moleExecution(capsule: ICapsule): IMoleExecution = new MoleExecution(new Mole(capsule))

  /**
   * Builds a Mole execution, that is to say a Mole ready to be run.
   *
   * @param puzzle,  the puzzle to be executed inside the Mole.
   * @return an instance of MoleExecution
   * @throws UserBadDataError
   * @throws InternalProcessingError
   * @throws InterruptedException
   */
  def moleExecution(puzzle: IPuzzleFirstAndLast): IMoleExecution = new MoleExecution(mole(puzzle))

  /**
   * Builds a Mole execution, that is to say a Mole ready to be run.
   *
   * @param mole, the mole to be executed.
   * @return an instance of MoleExecution.
   * @throws InternalProcessingError
   * @throws UserBadDataError
   */
  def moleExecution(mole: IMole): IMoleExecution = new MoleExecution(mole)
    

  /**
   * Builds a Mole execution, with a specific environment strategy.
   *
   * @param mole, the puzzle to be executed inside the Mole.
   * @return an instance of MoleExecution.
   * @throws InternalProcessingError
   * @throws UserBadDataError
   */
  def moleExecution(puzzle: IPuzzleFirstAndLast, strategy: IEnvironmentSelection): IMoleExecution = new MoleExecution(mole(puzzle), strategy)

  /**
   * Builds an environment selection object.
   * @return an instance of FixedEnvironmentSelection
   * @throws InternalProcessingError
   */
  def fixedEnvironmentSelection: FixedEnvironmentSelection = new FixedEnvironmentSelection


  /**
   * Builds a Factor according to a prototype
   *
   * @param prototype, the prototype to be
   * @param domain, to domain on which making the exploration
   * @return an instance of Factor
   */
  def factor[T, D <: IDomain[T]](prototype: IPrototype[T], domain: D): IFactor[T, D] = new Factor(prototype, domain)      

  /**
   * Builds an exploration task, according to a Design of Experiment.
   *
   * @param name, the name of the task,
   * @param sampler, the sampler to be explored.
   * @return an instance of ExplorationTask
   * @throws UserBadDataError
   * @throws InternalProcessingError
   */
  def explorationTask(name: String, sampler: ISampling): IExplorationTask = new ExplorationTask(name, sampler)
        

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
  def  explorationTask(name: String, sampler: ISampling, input: IDataSet): IExplorationTask = {
    val explo = new ExplorationTask(name, sampler)
    explo.addInput(input)
    explo
  }


  /**
   * Builds an transitin exploration from a exploration task capsule and a
   * task to be explored.
   *
   * @param explorationCapsule, the exploration task capsule.
   * @param exploredTask, the task to be explored.
   * @return an instance of Capsule
   */
  def explorationTransition(explorationCapsule: ICapsule, exploredTask: ITask): ITransition = {
    val exploredCapsule = new Capsule(exploredTask)
    new Transition(explorationCapsule, exploredCapsule)
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
  def explorationMoleTask(taskName: String, explo: IExplorationTask, puzzle: IPuzzleFirstAndLast) = {
        
    val ft = puzzle.last.task.getOrElse(throw new UserBadDataError("Task unasigned for first capsule of the puzzle"))
    
    // the final task making possible the retrieving of output
    //val inputToGlobalTask = new InputToGlobalTask(taskName + "InputToGlobalTask", ft.userOutputs.map{ Data.toArray(_)} )
    val exploPuz = exploration(explo, puzzle)
    
    // builds a mole containing a exploration, a puzzle, and an aggregation on the inputToGlobalTask
    new MoleTask(taskName, new Mole(exploPuz.first), puzzle.last)
  }
  
  
  def iterative(iterationName: String, nb: Int, head: ICapsule, capsules: Array[ICapsule] = Array.empty): IPuzzleFirstAndLast =
    iterative(iterationName, nb, chain(head, capsules))
  
    
  def iterative(iterationName: String, nb: Int, puzzle: IPuzzleFirstAndLast) = {
    val prototype = new Prototype(iterationName, classOf[Int])
    
    val loopOnTask = new EmptyTask(iterationName + "_loopOn")
    
    loopOnTask.addParameter(prototype, 0)
    loopOnTask.addInput(prototype)
    loopOnTask.addOutput(prototype)
    
    val loopOnCapsule = new StrainerCapsule(loopOnTask)

    val decrementCapsule = new StrainerCapsule(new Task {
        val name = iterationName + "_decrement"
        addInput(prototype)
        addOutput(prototype)
      
        override def process(context: IContext) =
          context + (prototype, context.value(prototype).get + 1)
      
      })
     
    new Transition(loopOnCapsule, puzzle.first)
    new Transition(puzzle.last, decrementCapsule)
    new Transition(decrementCapsule, new Slot(loopOnCapsule), prototype.name + "<=" + nb)
    new DataChannel(loopOnCapsule, decrementCapsule)
    new PuzzleFirstAndLast(loopOnCapsule, decrementCapsule)
  }
      
  /**
   * Creates a PuzzlleFirstAndLast from an ICapsule. It will be the integrated in a
   * more generic workflow as a puzzle.
   *
   * @param singleCapsule, the ICapsule to be encapsuled in a IPuzzleFirstAndLast
   * @return
   */
  def puzzle[T <: ICapsule](singleCapsule: T): IPuzzleFirstAndLast = new PuzzleFirstAndLast(singleCapsule, singleCapsule)
 
  def puzzle(singleTask: ITask): IPuzzleFirstAndLast = puzzle(new Capsule(singleTask))

  def puzzle(first: ICapsule, last: ICapsule): IPuzzleFirstAndLast = new PuzzleFirstAndLast(first, last)
  
  
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
    new PuzzleLast(last)
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
