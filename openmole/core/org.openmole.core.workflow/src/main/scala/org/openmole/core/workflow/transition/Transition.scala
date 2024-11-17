/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.transition

import cats.implicits._
import org.openmole.core.context.*
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.argument.Condition
import org.openmole.core.workflow.mole.MoleExecution.{ AggregationTransitionRegistryRecord, SubMoleExecutionState }
import org.openmole.core.workflow.mole.MoleExecutionMessage.PerformTransition
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation.TypeUtil._
import org.openmole.core.workflow.validation._

import scala.collection.mutable
import scala.collection.mutable.{ ArrayBuffer, HashSet, ListBuffer }
import scala.util.{ Failure, Success, Try }

object Transition {

  def isExploration(t: Transition) =
    t match
      case _: ExplorationTransition => true
      case _                        => false

  def isAggregation(t: Transition) =
    t match
      case _: AggregationTransition => true
      case _                        => false

  def isSlave(t: Transition) =
    t match
      case _: SlaveTransition => true
      case _                  => false

  def isEndExploration(t: Transition) =
    t match
      case _: EndExplorationTransition => true
      case _                           => false

  def nextTaskReady(end: TransitionSlot)(ticket: Ticket, registry: MoleExecution.TransitionRegistry, mole: Mole): Boolean = mole.inputTransitions(end).forall(registry.isRegistred(_, ticket))

  def submitNextJobsIfReady(transition: Transition)(context: Iterable[Variable[?]], ticket: Ticket, subMoleState: SubMoleExecutionState) =
    val mole = subMoleState.moleExecution.mole
    subMoleState.transitionRegistry.register(transition, ticket, context)
    if (nextTaskReady(transition.end)(ticket, subMoleState.transitionRegistry, mole))
    then

      def removeVariables(t: Transition) = subMoleState.transitionRegistry.remove(t, ticket).getOrElse(throw new InternalProcessingError("BUG context should be registered")).toIterable

      val transitionVariables: Iterable[Variable[?]] = mole.inputTransitions(transition.end).toList.flatMap { t => removeVariables(t) }

      val dataChannelVariables =
        lazy val transitionVariableNames = transitionVariables.map(_.prototype.name).toSet
        val variables = mole.inputDataChannels(transition.end).toList.flatMap { d => DataChannel.consums(d, ticket, subMoleState.moleExecution) }
        variables.filter(v => !transitionVariableNames.contains(v.name))

      val newTicket =
        if (mole.slots(transition.end.capsule).size <= 1) ticket
        else MoleExecution.nextTicket(subMoleState.moleExecution, ticket.parent.getOrElse(throw new InternalProcessingError("BUG should never reach root ticket")))

      val toArrayManifests = MoleExecution.cachedValidTypes(subMoleState.moleExecution, transition.end).filter(_.toArray).map(ct => Val(ct.name)(ct.`type`)).toSeq

      val combinasion = (dataChannelVariables ++ transitionVariables)

      val newContext = ContextAggregator.aggregate(MoleExecution.cachedCapsuleInputs(subMoleState.moleExecution, transition.end.capsule), toArrayManifests, combinasion)
      MoleExecution.submit(subMoleState, transition.end.capsule, newContext, newTicket)

}

/**
 * The trait representing a transition between a start point which is a [[org.openmole.core.workflow.mole.MoleCapsule]]
 * and an endpoint which is a [[org.openmole.core.workflow.transition.TransitionSlot]]
 */
sealed trait Transition {

  /**
   *
   * Get the starting capsule of this transition.
   *
   * @return the starting capsule of this transition
   */
  def start: MoleCapsule

  /**
   *
   * Get the ending capsule of this transition.
   *
   * @return the ending capsule of this transition
   */
  def end: TransitionSlot

  /**
   *
   * Get the filter of the variables which are filtered by this transition.
   *
   * @return filter on the names of the variables which are filtered by this transition
   */
  def filter: BlockList

  /**
   * Get the unfiltered user output data of the starting capsule going through
   * this transition
   *
   * @return the unfiltred output data of the staring capsule
   */
  def data(mole: Mole, sources: Sources, hooks: Hooks): PrototypeSet =
    start.outputs(mole, sources, hooks).filterNot(d => filter(d))

  /**
   *
   * Perform the transition and submit the jobs for the following capsules in the mole.
   *
   * @param ticket    ticket of the previous job
   * @param subMole   current submole
   */
  def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, moleExecutionContext: MoleExecutionContext): Unit

  /**
   * Filter a given context
   * @param context
   * @return
   */
  protected def filtered(context: Context): Context = context.values.filterNot { v => filter(v.prototype) }

  override def toString = this.getClass.getSimpleName + " from " + start + " to " + end

}

/**
 * Transition between a mole and a slot
 *
 * @param start
 * @param end
 * @param condition
 * @param filter
 */
class DirectTransition(
  val start:     MoleCapsule,
  val end:       TransitionSlot,
  val condition: Condition      = Condition.True,
  val filter:    BlockList      = BlockList.empty) extends Transition with ValidateTransition:

  override def validate = condition.validate

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, moleExecutionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    PerformTransition(subMole) { subMoleState =>
      import moleExecutionContext.services._
      if (condition.from(context)) Transition.submitNextJobsIfReady(this)(filtered(context).values, ticket, subMoleState)
    }
  }

  override def toString = s"$start -- $end"


object ExplorationTransition {

  def listAggregationTransitions(transition: ExplorationTransition, moleExecution: MoleExecution): Seq[AggregationTransition] =
    val alreadySeen = new HashSet[MoleCapsule]
    val toProcess = new ListBuffer[(MoleCapsule, Int)]
    toProcess += ((transition.end.capsule, 0))

    val result = ListBuffer[AggregationTransition]()
    while toProcess.nonEmpty
    do
      val cur = toProcess.remove(0)
      val capsule = cur._1
      val level = cur._2

      if !alreadySeen(capsule)
      then
        alreadySeen += capsule

        moleExecution.mole.outputTransitions(capsule).foreach:
          case t: AggregationTransition =>
            if level > 0
            then toProcess += t.end.capsule -> (level - 1)
            else
              if level == 0
              then result += t
//                subMoleExecution.aggregationTransitionRegistry.register(t, ticket, AggregationTransitionRegistryRecord(size))
//                subMoleExecution.onFinish += { se => AggregationTransition.aggregate(t, se, ticket, executionContext) }
          case t if Transition.isExploration(t) => toProcess += t.end.capsule -> (level + 1)
          case t                                => toProcess += t.end.capsule -> level

    result.toSeq

  def factors(capsule: MoleCapsule, moleExecution: MoleExecution) =
    val explored = ExplorationTask.explored(capsule, moleExecution.mole, moleExecution.sources, moleExecution.hooks)
    capsule.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks).partition(explored)
    val (factors, outputs) = capsule.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks).partition(explored)
    val typedFactors = factors.map(_.asInstanceOf[Val[Array[?]]])
    (typedFactors, outputs)

  object CompactedSampleList:
    def apply(vs: Iterable[Val[Array[?]]], context: Context) =
      new CompactedSampleList(vs.map(v => context(v)).toVector)

  class CompactedSampleList(arrays: Vector[Array[?]]) {

    def apply(index: Int) = arrays.map { a => a(index) }

    def size =
      arrays.headOption match
        case None    => 0
        case Some(a) => a.length

  }

  def exploredSamples(capsule: MoleCapsule, context: Context, moleExecution: MoleExecution) = CompactedSampleList(factors(capsule, moleExecution)._1, context)

  def submitIn(transition: Transition, condition: Condition, context: Context, ticket: Ticket, samples: CompactedSampleList, subMole: SubMoleExecutionState, executionContext: MoleExecutionContext) = {
    val moleExecution = subMole.moleExecution
    val mole = moleExecution.mole
    val (typedFactors, outputs) = factors(transition.start, moleExecution)
    val factorVals = typedFactors.map { f => Val.copyWithType(f, ValType.fromArrayUnsecure(f.`type`)) }

    for (i <- 0 until samples.size) {
      val value = samples(i)
      val newTicket = MoleExecution.nextTicket(moleExecution, ticket)
      val variables = new ListBuffer[Variable[?]]

      for (in <- outputs)
        context.variable(in) match {
          case Some(v) => variables += v
          case None    =>
        }

      for ((fv, v) <- factorVals zip value) {
        if (fv.accepts(v)) variables += Variable(fv, v)
        else throw new UserBadDataError("Found value of type " + v.asInstanceOf[AnyRef].getClass + " incompatible with prototype " + fv)
      }

      import executionContext.services._

      if (condition.from(variables)) { Transition.submitNextJobsIfReady(transition)(ListBuffer() ++ variables, newTicket, subMole) }
    }

  }

}

class ExplorationTransition(val start: MoleCapsule, val end: TransitionSlot, val condition: Condition = Condition.True, val filter: BlockList = BlockList.empty) extends Transition with ValidateTransition {

  override def validate = condition.validate

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution):
    MoleExecutionMessage.PerformTransition(subMole): subMoleState =>
      val samples = ExplorationTransition.exploredSamples(start, context, moleExecution)

      val aggregationTransitions = ExplorationTransition.listAggregationTransitions(this, moleExecution)

      def onFinish =
        IArray.unsafeFromArray:
          aggregationTransitions.toArray.map: t =>
            AggregationTransition.aggregate(t, _, ticket, executionContext)

      val subSubMole = MoleExecution.newChildSubMoleExecution(subMoleState, onFinish)

      for
        t <- aggregationTransitions
      do subSubMole.aggregationTransitionRegistry.register(t, ticket, AggregationTransitionRegistryRecord(samples.size))

      ExplorationTransition.submitIn(this, condition, filtered(context), ticket, samples, subSubMole, executionContext)

  override def toString = s"$start -< $end"

}

object AggregationTransition {

  def aggregatedOutputs(moleExecution: MoleExecution, transition: AggregationTransition) = transition.start.outputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks).toVector

  def aggregateOutputs(moleExecution: MoleExecution, transition: AggregationTransition, results: AggregationTransitionRegistryRecord): Context =
    val vals = aggregatedOutputs(moleExecution, transition)

    val resultValues = results.values.value
    val size = resultValues.size

    def resultsArrays = (resultValues zip results.ids.value).sortBy(_._2).map(_._1).transpose

    def variables = (resultsArrays zip vals).map: (values, v) =>
      val result = v.`type`.manifest.newArray(values.size)
      var i = 0
      for
        x <- values
      do
        java.lang.reflect.Array.set(result, i, x)
        i += 1

      Variable.unsecure(v, result)


    new collection.mutable.WrappedArray.ofRef(variables)

  def aggregate(aggregationTransition: AggregationTransition, subMole: SubMoleExecutionState, ticket: Ticket, executionContext: MoleExecutionContext) = {
    import executionContext.services._

    if ( /*!subMole.canceled && */ !hasBeenPerformed(aggregationTransition, subMole, ticket)) {
      val results = subMole.aggregationTransitionRegistry.remove(aggregationTransition, ticket).getOrElse(throw new InternalProcessingError("No context registered for the aggregation transition"))
      val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("SubMole execution has no parent"))
      val aggregated = aggregateOutputs(subMole.moleExecution, aggregationTransition, results)
      if (aggregationTransition.condition.from(aggregated)) Transition.submitNextJobsIfReady(aggregationTransition)(aggregated.values, ticket, subMoleParent)
    }
  }

  def hasBeenPerformed(aggregationTransition: AggregationTransition, subMole: SubMoleExecutionState, ticket: Ticket): Boolean = !subMole.aggregationTransitionRegistry.isRegistred(aggregationTransition, ticket)

  def allAggregationTransitionsPerformed(aggregationTransition: AggregationTransition, subMole: SubMoleExecutionState, ticket: Ticket) =
    import scala.util.boundary
    def oneAggregationTransitionNotPerformed(subMole: SubMoleExecutionState, ticket: Ticket): Boolean = boundary:
      val mole = subMole.moleExecution.mole
      val alreadySeen = new HashSet[MoleCapsule]
      val toProcess = new ListBuffer[(MoleCapsule, Int)]
      toProcess += ((aggregationTransition.start, 0))

      while (!toProcess.isEmpty)
        val (capsule, level) = toProcess.remove(0)

        if !alreadySeen(capsule)
        then
          alreadySeen += capsule
          mole.slots(capsule).toList.flatMap { mole.inputTransitions }.foreach:
            case t if Transition.isExploration(t) => if (level > 0) toProcess += ((t.start, level - 1))
            case t: AggregationTransition =>
              if (level == 0 && t != aggregationTransition && !hasBeenPerformed(t, subMole, ticket)) boundary.break(true)
              toProcess += ((t.start, level + 1))
            case t => toProcess += ((t.start, level))

          mole.outputTransitions(capsule).foreach:
            case t if Transition.isExploration(t) => toProcess += ((t.end.capsule, level + 1))
            case t: AggregationTransition =>
              if (level == 0 && t != aggregationTransition && !hasBeenPerformed(t, subMole, ticket)) boundary.break(true)
              if (level > 0) toProcess += ((t.end.capsule, level - 1))
            case t => toProcess += ((t.end.capsule, level))

      false

    !oneAggregationTransitionNotPerformed(subMole, ticket)

}

class AggregationTransition(val start: MoleCapsule, val end: TransitionSlot, val condition: Condition = Condition.True, val filter: BlockList = BlockList.empty, val trigger: Condition = Condition.False) extends Transition with ValidateTransition {

  override def validate = condition.validate ++ trigger.validate

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    MoleExecutionMessage.PerformTransition(subMole) { subMoleState =>
      import executionContext.services._
      val moleExecution = subMoleState.moleExecution
      val mole = moleExecution.mole
      val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration."))

      if ( /*!subMole.canceled && */ !AggregationTransition.hasBeenPerformed(this, subMoleState, parentTicket)) {
        subMoleState.aggregationTransitionRegistry.consult(this, parentTicket) match {
          case Some(results) =>
            results.ids.append(ticket.content)
            results.values.append(AggregationTransition.aggregatedOutputs(moleExecution, this).map(v => context(v)).toArray)

            if (trigger != Condition.False) {
              val context = AggregationTransition.aggregateOutputs(moleExecution, this, results)
              if (trigger.from(context)) {
                val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("Aggregation transition should take place after an exploration"))
                val subMoleParent = subMoleState.parent.getOrElse(throw new InternalProcessingError("SubMoleExecution has no parent"))
                AggregationTransition.aggregate(this, subMoleParent, parentTicket, executionContext)
                if (AggregationTransition.allAggregationTransitionsPerformed(this, subMoleState, parentTicket)) MoleExecution.cancel(subMoleState)
              }
            }

          case None => throw new InternalProcessingError("No context registered for aggregation.")
        }
      }
    }
  }

  override def toString = s"$start >- $end"
}

class EndExplorationTransition(val start: MoleCapsule, val end: TransitionSlot, val trigger: Condition, val filter: BlockList = BlockList.empty) extends Transition with ValidateTransition {

  override def validate = trigger.validate

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    MoleExecutionMessage.PerformTransition(subMole) { subMoleState =>
      def perform() = {
        val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("End exploration transition should take place after an exploration."))
        val subMoleParent = subMoleState.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))
        //subMoleParent.transitionLock { ITransition.submitNextJobsIfReady(this)(context.values, parentTicket, subMoleParent) }
        Transition.submitNextJobsIfReady(this)(context.values, parentTicket, subMoleParent)
        MoleExecution.cancel(subMoleState) //.cancel
      }

      import executionContext.services._

      Try( /*!subMoleState.canceled && */ trigger.from(context)) match {
        case Success(true)  => perform()
        case Success(false) =>
        case Failure(t) =>
          MoleExecution.cancel(subMoleState)
          throw t
      }
    }
  }

  override def toString = s"$start >| $end"
}

class SlaveTransition(val start: MoleCapsule, val end: TransitionSlot, val condition: Condition = Condition.True, val filter: BlockList = BlockList.empty, val slaves: Option[Int] = None) extends Transition with ValidateTransition {

  override def validate = condition.validate

  override def perform(context: Context, ticket: Ticket, moleExecution: MoleExecution, subMole: SubMoleExecution, executionContext: MoleExecutionContext) = MoleExecutionMessage.send(moleExecution) {
    MoleExecutionMessage.PerformTransition(subMole) { subMoleState =>
      import executionContext.services._

      if (condition.from(context) && slaves.map(subMoleState.jobs.size < _).getOrElse(true)) {
        val samples = ExplorationTransition.exploredSamples(start, context, moleExecution)

        ExplorationTransition.submitIn(
          this,
          condition,
          filtered(context),
          ticket.parent.getOrElse(throw new UserBadDataError("Slave transition should take place within an exploration.")),
          samples,
          subMoleState,
          executionContext)
      }
    }
  }
  override def toString = s"$start -<- $end"

}

