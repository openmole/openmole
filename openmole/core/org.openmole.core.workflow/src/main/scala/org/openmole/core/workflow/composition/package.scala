/*
 * Copyright (C) 2019 Romain Reuillon
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

package org.openmole.core.workflow.composition

/**
 * This package articulates workflows, by providing <code>Puzzle</code> and the bricks to construct them such as transitions.
 *
 * It also contains a large part of the DSL.
 */

import java.io.PrintStream
import org.openmole.core.context.{Context, Val}
import org.openmole.core.argument.{Condition, FromContext, OptionalArgument, Validate}
import org.openmole.core.keyword.{By, On}
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.composition.DSL.{ToDestination, ToOrigin}
import org.openmole.core.workflow.execution.{EnvironmentBuilder, LocalEnvironmentBuilder}
import org.openmole.core.workflow.hook.{Hook, OMRFileHook}
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.sampling.Sampling
import org.openmole.core.workflow.task.{EmptyTask, ExplorationTask, MoleTask, Task}
import org.openmole.core.workflow.transition.*
import org.openmole.core.workflow.validation.TypeUtil
import org.openmole.core.format.*
import org.openmole.core.format.WritableOutput.Display
import org.openmole.core.script.ScriptSourceData

object Puzzle:

  def apply(
    firstSlot:    TransitionSlot,
    lasts:        Iterable[MoleCapsule],
    transitions:  Iterable[Transition]                  = Iterable.empty,
    dataChannels: Iterable[DataChannel]                 = Iterable.empty,
    sources:      Iterable[(MoleCapsule, Source)]       = Iterable.empty,
    hooks:        Iterable[(MoleCapsule, Hook)]         = Iterable.empty,
    environments: Map[MoleCapsule, EnvironmentBuilder] = Map.empty,
    grouping:     Map[MoleCapsule, Grouping]            = Map.empty,
    validate:     Validate                              = Validate.success): Puzzle =
    new Puzzle(
      firstSlot = firstSlot,
      lasts = lasts,
      transitions = transitions,
      dataChannels = dataChannels,
      sources = sources,
      hooks = hooks,
      environments = environments,
      grouping = grouping,
      validate = validate
    )

  def apply(s: TransitionSlot): Puzzle = Puzzle(s, lasts = Vector(s.capsule))

  def copy(puzzle: Puzzle)(
    firstSlot:    TransitionSlot                        = puzzle.firstSlot,
    lasts:        Iterable[MoleCapsule]                 = puzzle.lasts,
    transitions:  Iterable[Transition]                  = puzzle.transitions,
    dataChannels: Iterable[DataChannel]                 = puzzle.dataChannels,
    sources:      Iterable[(MoleCapsule, Source)]       = puzzle.sources,
    hooks:        Iterable[(MoleCapsule, Hook)]         = puzzle.hooks,
    environments: Map[MoleCapsule, EnvironmentBuilder] = puzzle.environments,
    grouping:     Map[MoleCapsule, Grouping]            = puzzle.grouping,
    validate:     Validate                              = puzzle.validate
  ) = apply(
    firstSlot,
    lasts,
    transitions,
    dataChannels,
    sources,
    hooks,
    environments,
    grouping,
    validate
  )

  /**
   * Merge two puzzles. The entry slot is taken as the entry slot of p1.
   * It concatenates transitions, data channels, environments and groupings; concatenates and reduce to distinct elements sources and hooks.
   * Last capsules are either merged, or taken as p1 depending on the argument mergeLast.
   *
   * @param p1
   * @param p2
   * @param mergeLasts
   * @return
   */
  def merge(p1: Puzzle, p2: Puzzle, mergeLasts: Boolean = false) =
    Puzzle(
      p1.firstSlot,
      if (!mergeLasts) p1.lasts else p1.lasts ++ p2.lasts,
      p1.transitions.toList ::: p2.transitions.toList,
      p1.dataChannels.toList ::: p2.dataChannels.toList,
      (p1.sources.toList ::: p2.sources.toList).distinct,
      (p1.hooks.toList ::: p2.hooks.toList).distinct,
      p1.environments ++ p2.environments,
      p1.grouping ++ p2.grouping,
      p1.validate ++ p2.validate
    )

  def add(
    p:            Puzzle,
    transitions:  Iterable[Transition]                  = Iterable.empty,
    dataChannels: Iterable[DataChannel]                 = Iterable.empty,
    sources:      Iterable[(MoleCapsule, Source)]       = Iterable.empty,
    hooks:        Iterable[(MoleCapsule, Hook)]         = Iterable.empty,
    environments: Map[MoleCapsule, EnvironmentBuilder] = Map.empty,
    grouping:     Map[MoleCapsule, Grouping]            = Map.empty,
    validate:     Validate                              = Validate.success) =
    copy(p)(
      transitions = p.transitions ++ transitions,
      dataChannels = p.dataChannels ++ dataChannels,
      sources = p.sources ++ sources,
      hooks = p.hooks ++ hooks,
      environments = p.environments ++ environments,
      grouping = p.grouping ++ grouping,
      validate = p.validate ++ validate
    )

  def toMole(p: Puzzle) = new Mole(p.firstSlot.capsule, p.transitions, p.dataChannels, validate = p.validate)


/**
 * A Puzzle is the overall wrapper of the different components of an experiment: it articulates the different <code>MoleCapsule</code> in the script
 * through transitions between slots, and incorporates data, sources, hooks, environments.
 *
 * @param firstSlot the first slot to enter the puzzle
 * @param lasts the set of MoleCapsule on which the puzzle ends
 * @param transitions the transitions from which the workflow is reconstructed
 * @param dataChannels
 * @param sources sources (mapped as MoleCapsule -> Hook)
 * @param hooks hooks (mapped as MoleCapsule -> Hook)
 * @param environments
 * @param grouping
 */
class Puzzle(
  val firstSlot:    TransitionSlot,
  val lasts:        Iterable[MoleCapsule],
  val transitions:  Iterable[Transition],
  val dataChannels: Iterable[DataChannel],
  val sources:      Iterable[(MoleCapsule, Source)],
  val hooks:        Iterable[(MoleCapsule, Hook)],
  val environments: Map[MoleCapsule, EnvironmentBuilder],
  val grouping:     Map[MoleCapsule, Grouping],
  val validate:     Validate):

  def slots: Set[TransitionSlot] = (firstSlot :: transitions.map(_.end).toList).toSet
  def first = firstSlot.capsule
  def inputs = first.inputs(Puzzle.toMole(this), sources, hooks).toSeq
  def defaults = Task.defaults(first.task(Puzzle.toMole(this), sources, hooks))


case class SingleTaskMethod()

case class TaskNode(task: Task, strain: Boolean = false, funnel: Boolean = false, master: Boolean = false, persist: Seq[Val[?]] = Seq.empty, environment: Option[EnvironmentBuilder] = None, grouping: Option[Grouping] = None, hooks: Vector[Hook] = Vector.empty, sources: Vector[Source] = Vector.empty):
  infix def hook(hooks: Hook*) = copy(hooks = this.hooks ++ hooks)
  infix def hook(
    output: WritableOutput,
    values: Seq[Val[?]]    = Vector.empty)(using DefinitionScope, ScriptSourceData): TaskNode = hook(OMRFileHook(output = output, values = values, option = OMROption(append = true), metadata = None))
  infix def source(sources: Source*) = copy(sources = this.sources ++ sources)


object TransitionOrigin:
  def tasks(d: TransitionOrigin) =
    d match 
      case TaskOrigin(n)          => Vector(n)
      case TransitionDSLOrigin(o) => DSL.tasks(o)

  def dsl(o: TransitionOrigin) =
    o match
      case TransitionDSLOrigin(d) => Some(d)
      case _                      => None



sealed trait TransitionOrigin:
  infix def --(d1: TransitionDestination, d: TransitionDestination*) = new --(this, (Seq(d1) ++ d).toVector)
  infix def --(d: Seq[TransitionDestination]) = new --(this, d.toVector)

  infix def -<(d: TransitionDestination*) = new -<(this, d.toVector)
  infix def >-(d: TransitionDestination*) = new >-(this, d.toVector)
  infix def >|(d: TransitionDestination*) = new >|(this, d.toVector)
  infix def -<-(d: TransitionDestination*) = new -<-(this, d.toVector)

  infix def oo(d: TransitionDestination*) = new oo(this, d.toVector)

case class TaskOrigin(node: TaskNode) extends TransitionOrigin
case class TransitionDSLOrigin(t: DSL) extends TransitionOrigin

object TransitionDestination:

  def tasks(d: TransitionDestination) =
    d match
      case TaskDestination(n)          => Vector(n)
      case TransitionDSLDestination(t) => DSL.tasks(t)

  def dsl(o: TransitionDestination) =
    o match
      case TransitionDSLDestination(d) => Some(d)
      case _                           => None


sealed trait TransitionDestination
case class TaskDestination(node: TaskNode) extends TransitionDestination
case class TransitionDSLDestination(t: DSL) extends TransitionDestination

object DSL {

  def tasks(t: DSL): Vector[TaskNode] =
    t match
      case --(o, d, _, _)     => TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case -<(o, d, _, _)     => TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case >-(o, d, _, _)     => TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case >|(o, d, _, _)     => TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case -<-(o, d, _, _, _) => TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case oo(o, d, _)        => TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case &(a, b)            => tasks(a) ++ tasks(b)
      case Slot(d)            => tasks(d)
      case Capsule(d, _)      => tasks(d)
      case c: DSLContainer[?] => DSLContainer.taskNodes(c) ++ tasks(c.dsl)
      case TaskNodeDSL(n)     => Vector(n)

  def delegate(t: DSL): Vector[Task] =
    def innerDSL(t: DSL): Seq[DSL] =
      t match
        case c: DSLContainer[?] => Seq(c.dsl)
        case --(o, d, _, _)     => TransitionOrigin.dsl(o).toSeq ++ d.flatMap(TransitionDestination.dsl)
        case -<(o, d, _, _)     => TransitionOrigin.dsl(o).toSeq ++ d.flatMap(TransitionDestination.dsl)
        case >-(o, d, _, _)     => TransitionOrigin.dsl(o).toSeq ++ d.flatMap(TransitionDestination.dsl)
        case >|(o, d, _, _)     => TransitionOrigin.dsl(o).toSeq ++ d.flatMap(TransitionDestination.dsl)
        case -<-(o, d, _, _, _) => TransitionOrigin.dsl(o).toSeq ++ d.flatMap(TransitionDestination.dsl)
        case oo(o, d, _)        => TransitionOrigin.dsl(o).toSeq ++ d.flatMap(TransitionDestination.dsl)
        case &(a, b)            => innerDSL(a) ++ innerDSL(b)
        case Slot(d)            => innerDSL(d)
        case Capsule(d, _)      => innerDSL(d)
        case TaskNodeDSL(n)     => Seq()


    def toDelegate(t: DSL): Seq[Task] =
      t match
        case c: DSLContainer[?] if c.delegate.nonEmpty => c.delegate
        case c => innerDSL(c).flatMap(toDelegate)

    val del =toDelegate(t)
    if del.nonEmpty
    then del.toVector
    else tasks(t).map(_.task)

  object DSLSelector:
    //def apply[T](implicit selector: DSLSelector[T]): DSLSelector[T] = selector
    //implicit def select[T <: HList]: DSLSelector[DSL :: T] = (l: DSL :: T) => l.head
    //implicit def recurse[H, T <: HList](implicit st: DSLSelector[T]): DSLSelector[H :: T] = (l: H :: T) => st(l.tail)
    given [T](using select: org.openmole.tool.types.SelectTuple[DSL, T]): DSLSelector[T] = t => select.select(t)

  trait DSLSelector[T]:
    def select(t: T): DSL

  /* ----------- implicit conversions ----------------- */

  object ToOrigin:
    import org.openmole.core.workflow.sampling.IsSampling
    given ToOrigin[Task] = t => TaskOrigin(TaskNode(t))
    given ToOrigin[DSL] = TransitionDSLOrigin(_)
    given [T](using tc: org.openmole.core.workflow.composition.DSLContainer.ExplorationMethod[T, ?]): ToOrigin[T] = t => TransitionDSLOrigin(tc(t))
    given [T: ToTaskNode]: ToOrigin[T] = t => TaskOrigin(summon[ToTaskNode[T]].apply(t))
    given [T: IsSampling](using scope: DefinitionScope): ToOrigin[T] = s => summon[ToOrigin[Task]](ExplorationTask(s))

  trait ToOrigin[-T]:
    def apply(t: T): TransitionOrigin

  object ToDestination:
    import org.openmole.core.workflow.sampling.IsSampling
    given ToDestination[Task] = t => TaskDestination(TaskNode(t))
    given [T: ToTaskNode]: ToDestination[T] = t => TaskDestination(implicitly[ToTaskNode[T]].apply(t))
    given [T: IsSampling](using scope: DefinitionScope): ToDestination[T] = s => summon[ToDestination[Task]](ExplorationTask(s))
    given ToDestination[DSL] = TransitionDSLDestination(_)
    given [T](using tc: org.openmole.core.workflow.composition.DSLContainer.ExplorationMethod[T, ?]): ToDestination[T] = t => TransitionDSLDestination(tc(t))

  trait ToDestination[-T]:
    def apply(t: T): TransitionDestination

  object ToTaskNode:
    given [T <: Task]: ToTaskNode[T] = t => TaskNode(t)
    given ToTaskNode[TaskNode] = t => t
    given byToNode[T: ToTaskNode]: ToTaskNode[By[T, Grouping]] = t => summon[ToTaskNode[T]](t.value).copy(grouping = Some(t.by))
    given [T: ToTaskNode]: ToTaskNode[On[T, EnvironmentBuilder]] = t => summon[ToTaskNode[T]](t.value).copy(environment = Some(t.on))

  trait ToTaskNode[-T]:
    def apply(t: T): TaskNode

  object ToDSL:
    given ToDSL[DSL] = t => t
    given [T](using tc: org.openmole.core.workflow.composition.DSLContainer.ExplorationMethod[T, ?]): ToDSL[T] = t => tc(t)
    given [T: ToTaskNode]: ToDSL[T] = t => TaskNodeDSL(summon[ToTaskNode[T]](t))
    given [T](using dslSelector: DSLSelector[T]): ToDSL[T] = t => dslSelector.select(t)

  trait ToDSL[-T]:
    def apply(t: T): DSL

  object ToMole:
    given ToMole[DSL] = t => Puzzle.toMole(DSL.toPuzzle(t))
    given [T: ToDSL]: ToMole[T] = t => summon[ToMole[DSL]](summon[ToDSL[T]](t))

  trait ToMole[-T]:
    def apply(t: T): Mole

  object ToMoleExecution:
    given [T: ToDSL](using moleServices: MoleServices): ToMoleExecution[T] = t => MoleExecution(implicitly[ToDSL[T]].apply(t))

  trait ToMoleExecution[-T]:
    def apply(t: T): MoleExecution

  def toPuzzle(t: DSL): Puzzle =
    val taskNodeList = DSL.tasks(t)
    val taskEnvironment = taskNodeList.groupBy(n => n.task).view.mapValues(_.flatMap(_.environment).headOption).toMap
    val taskGrouping = taskNodeList.groupBy(n => n.task).view.mapValues(_.flatMap(_.grouping).headOption).toMap

    def taskToSlot(dsl: DSL) =
      def buildCapsule(task: Task, ns: Vector[TaskNode]) =
        val strain = ns.exists(_.strain)
        val funnel = ns.exists(_.funnel)
        val master = ns.exists(_.master)

        if !master
        then MoleCapsule(task, strain = strain, funnel = funnel)
        else
          val persist = ns.find(_.master).get.persist
          MasterCapsule(task, persist = persist, strain = strain)

      DSL.tasks(dsl).
        groupBy(_.task).map: (t, ns) =>
          t -> TransitionSlot(buildCapsule(t, ns))

    def taskNodeToPuzzle(n: TaskNode, slots: Map[Task, TransitionSlot]) =
      val task = n.task
      val slot = slots(task)
      val puzzle = Puzzle(slot)

      Puzzle.add(
        puzzle,
        environments = taskEnvironment(n.task).map(slot.capsule -> _).toMap,
        grouping = taskGrouping(n.task).map(slot.capsule -> _).toMap,
        hooks = n.hooks.map(slot.capsule -> _),
        sources = n.sources.map(slot.capsule -> _)
      )

    def transitionDSLToPuzzle0(t: DSL, slots: Map[Task, TransitionSlot], converted: collection.mutable.Map[DSL, Puzzle]): Puzzle =
      def transitionOriginToPuzzle(d: TransitionOrigin): Puzzle =
        d match
          case TaskOrigin(n)          => taskNodeToPuzzle(n, slots)
          case TransitionDSLOrigin(o) => transitionDSLToPuzzle0(o, slots, converted)

      def transitionDestinationToPuzzle(d: TransitionDestination): (Puzzle, Boolean) =
        d match
          case TaskDestination(n)                => taskNodeToPuzzle(n, slots) -> false
          case TransitionDSLDestination(t: Slot) => transitionDSLToPuzzle0(t, slots, converted) -> true
          case TransitionDSLDestination(t)       => transitionDSLToPuzzle0(t, slots, converted) -> false

      def transitionsToPuzzle[T](o: TransitionOrigin, d: Vector[TransitionDestination], add: (Puzzle, Iterable[T]) => Puzzle)(transition: (MoleCapsule, TransitionSlot) => T) =
        val originPuzzle = transitionOriginToPuzzle(o)
        val destinationPuzzle = d.map(transitionDestinationToPuzzle)

        val transitions =
          for
            l <- originPuzzle.lasts
            (f, newSlot) <- destinationPuzzle
          yield transition(l, if (!newSlot) f.firstSlot else TransitionSlot(f.first))

        val merged =
          destinationPuzzle.map(_._1).foldLeft(originPuzzle): (a, b) =>
            Puzzle.merge(a, b)

        val plugged = Puzzle.copy(merged)(firstSlot = originPuzzle.firstSlot, lasts = destinationPuzzle.map(_._1).flatMap(_.lasts))
        add(plugged, transitions)

      def dslContainerToPuzzle(container: DSLContainer[?]) =
        val puzzle = transitionDSLToPuzzle0(container.dsl, slots, converted)

        def outputs(dsl: DSL): Iterable[MoleCapsule] =
          dsl match
            case c: DSLContainer[?] if c.output.isDefined => Vector(slots(c.output.get).capsule)
            case c: DSLContainer[?]                       => outputs(c.dsl)
            case dsl                                      => transitionDSLToPuzzle0(dsl, slots, converted).lasts

        val hooks =
          for
            o <- outputs(container)
            h <- container.hooks
          yield o -> h

        val environments =
          for
            c <- container.delegate.map(d => slots(d).capsule)
            e <- container.environment
          yield c -> e

        Puzzle.add(
          puzzle,
          hooks = hooks,
          environments = environments.toMap,
          validate = container.validate
        )

      def addTransitions(p: Puzzle, transitions: Iterable[Transition]) = Puzzle.add(p, transitions = transitions)
      def addDataChannel(p: Puzzle, channels: Iterable[DataChannel]) = Puzzle.add(p, dataChannels = channels)

      def p =
        t match
          case --(o, d, condition, filter)          => transitionsToPuzzle(o, d, addTransitions) { case (c, s) => new DirectTransition(c, s, condition, filter) }
          case -<(o, d, condition, filter)          => transitionsToPuzzle(o, d, addTransitions) { case (c, s) => new ExplorationTransition(c, s, condition, filter) }
          case >-(o, d, condition, filter)          => transitionsToPuzzle(o, d, addTransitions) { case (c, s) => new AggregationTransition(c, s, condition, filter) }
          case >|(o, d, condition, filter)          => transitionsToPuzzle(o, d, addTransitions) { case (c, s) => new EndExplorationTransition(c, s, condition, filter) }
          case -<-(o, d, condition, filter, slaves) => transitionsToPuzzle(o, d, addTransitions) { case (c, s) => new SlaveTransition(c, s, condition, filter, slaves = slaves) }
          case oo(o, d, filter)                     => transitionsToPuzzle(o, d, addDataChannel) { case (c, s) => DataChannel(c, s, filter) }
          case &(a, b)                              => Puzzle.merge(transitionDSLToPuzzle0(a, slots, converted), transitionDSLToPuzzle0(b, slots, converted))
          case c: DSLContainer[?]                   => dslContainerToPuzzle(c)
          case TaskNodeDSL(n)                       => taskNodeToPuzzle(n, slots)
          case Capsule(d, _)                        => transitionDSLToPuzzle0(d, taskToSlot(d), collection.mutable.Map.empty) //taskNodeToPuzzle(n, taskToSlot(c))
          case Slot(d)                              => transitionDSLToPuzzle0(d, slots, converted)

      converted.getOrElseUpdate(t, p)


    transitionDSLToPuzzle0(t, taskToSlot(t), collection.mutable.Map())
}

/* -------------------- Transition DSL ---------------------- */
sealed trait DSL

case class --(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL:
  infix def when(condition: Condition) = copy(condition = condition)
  infix def filter(filter: BlockList) = copy(filterValue = filter)

  infix def keep(prototypes: Val[?]*) = filter(Keep(prototypes *))
  infix def keepAll(prototypes: Seq[Val[?]]) = filter(Keep(prototypes *))
  infix def block(prototypes: Val[?]*) = filter(Block(prototypes *))
  infix def blockAll(prototypes: Seq[Val[?]]) = filter(Block(prototypes *))

case class -<(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL:
  infix def when(condition: Condition) = copy(condition = condition)
  infix def filter(filter: BlockList) = copy(filterValue = filter)
  infix def keep(prototypes: Val[?]*) = filter(Keep(prototypes *))
  infix def keepAll(prototypes: Seq[Val[?]]) = filter(Keep(prototypes *))
  infix def block(prototypes: Val[?]*) = filter(Block(prototypes *))
  infix def blockAll(prototypes: Seq[Val[?]]) = filter(Block(prototypes *))

case class >-(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL:
  infix def when(condition: Condition) = copy(condition = condition)
  infix def filter(filter: BlockList) = copy(filterValue = filter)
  infix def keep(prototypes: Val[?]*) = filter(Keep(prototypes *))
  infix def keepAll(prototypes: Seq[Val[?]]) = filter(Keep(prototypes *))
  infix def block(prototypes: Val[?]*) = filter(Block(prototypes *))
  infix def blockAll(prototypes: Seq[Val[?]]) = filter(Block(prototypes *))

case class >|(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL:
  infix def when(condition: Condition) = copy(condition = condition)
  infix def filter(filter: BlockList) = copy(filterValue = filter)
  infix def keep(prototypes: Val[?]*) = filter(Keep(prototypes *))
  infix def keepAll(prototypes: Seq[Val[?]]) = filter(Keep(prototypes *))
  infix def block(prototypes: Val[?]*) = filter(Block(prototypes *))
  infix def blockAll(prototypes: Seq[Val[?]]) = filter(Block(prototypes *))

case class -<-(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty, slaves: Option[Int] = None) extends DSL:
  infix def when(condition: Condition) = copy(condition = condition)
  infix def filter(filter: BlockList) = copy(filterValue = filter)
  infix def keep(prototypes: Val[?]*) = filter(Keep(prototypes *))
  infix def keepAll(prototypes: Seq[Val[?]]) = filter(Keep(prototypes *))
  infix def block(prototypes: Val[?]*) = filter(Block(prototypes *))
  infix def blockAll(prototypes: Seq[Val[?]]) = filter(Block(prototypes *))
  infix def slaves(n: Int) = copy(slaves = Some(n))
  infix def slaves(n: Option[Int]) = copy(slaves = n)

case class oo(a: TransitionOrigin, b: Vector[TransitionDestination], filterValue: BlockList = BlockList.empty) extends DSL:
  infix def filter(filter: BlockList) = copy(filterValue = filter)
  infix def keep(prototypes: Val[?]*) = filter(Keep(prototypes *))
  infix def keepAll(prototypes: Seq[Val[?]]) = filter(Keep(prototypes *))
  infix def block(prototypes: Val[?]*) = filter(Block(prototypes *))
  infix def blockAll(prototypes: Seq[Val[?]]) = filter(Block(prototypes *))


case class &(a: DSL, b: DSL) extends DSL

object DSLContainer:
  def taskNodes(container: DSLContainer[?]) =
    val output = container.output.map { o => TaskNode(o, hooks = container.hooks) }
    val delegate = container.delegate.map { t => TaskNode(t, environment = container.environment, grouping = container.grouping) }
    delegate ++ output

  object ExplorationMethod:
    given [T]: ExplorationMethod[DSLContainer[T], T] = t => t
    given byGrouping[T, C](using toDSLContainer: ExplorationMethod[T, C]): ExplorationMethod[By[T, Grouping], C] = t => toDSLContainer(t.value).copy(grouping = Some(t.by))
    given on[T, C](using toDSLContainer: ExplorationMethod[T, C]): ExplorationMethod[On[T, EnvironmentBuilder], C] = t => toDSLContainer(t.value).copy(environment = Some(t.on))
    given hooked[T, C](using toDSLContainer: ExplorationMethod[T, C]): ExplorationMethod[Hooked[T], C] = t =>
      val container = toDSLContainer(t.value)
      container.copy(hooks = container.hooks ++ Seq(t.h))

    def apply[T, D](f: T => DSLContainer[D]) =
      new ExplorationMethod[T, D]:
        def apply(t: T) = f(t)

  trait ExplorationMethod[-T, +D]:
    def apply(t: T): DSLContainer[D]

  implicit def convert[T, C](t: T)(implicit toDSLContainer: ExplorationMethod[T, C]): DSLContainer[C] = toDSLContainer(t)


case class DSLContainer[+T](
  dsl:         DSL,
  output:      Option[Task]                = None,
  delegate:    Vector[Task]                = Vector.empty,
  environment: Option[EnvironmentBuilder] = None,
  grouping:    Option[Grouping]            = None,
  hooks:       Vector[Hook]                = Vector.empty,
  validate:    Validate                    = Validate.success,
  method:      T,
  scope:       DefinitionScope) extends DSL

case class TaskNodeDSL(node: TaskNode) extends DSL
case class Slot(dsl: DSL) extends DSL
case class Capsule(dsl: DSL, id: Any = new Object) extends DSL

object ExplorationMethodSetter:
  implicit infix def by[T, B, P](using isContainer: ExplorationMethodSetter[T, P]): ExplorationMethodSetter[By[T, B], P] = (t, h) => t.copy(value = isContainer(t.value, h))
  implicit infix def on[T, B, P](using isContainer: ExplorationMethodSetter[T, P]): ExplorationMethodSetter[On[T, B], P] = (t, h) => t.copy(value = isContainer(t.value, h))
  implicit infix def hooked[T, P](using isContainer: ExplorationMethodSetter[T, P]): ExplorationMethodSetter[Hooked[T], P] = (t, h) => t.copy(value = isContainer(t.value, h))

trait ExplorationMethodSetter[T, P]:
  def apply(t: T, h: P): T

case class Hooked[+T](value: T, h: Hook)

trait CompositionPackage {

  export org.openmole.core.workflow.composition.DSL

  class MethodHookDecorator[T, C](dsl: T)(implicit method: ExplorationMethod[T, C]):
    def hook(hook: Hook): Hooked[T] = Hooked(dsl, hook)

  implicit def hookDecorator[T](container: T)(implicit method: ExplorationMethod[T, Unit]): MethodHookDecorator[T, Unit] = new MethodHookDecorator[T, Unit](container)

  export org.openmole.core.workflow.composition.ExplorationMethodSetter

  def DSLContainer[T](
    dsl:         DSL,
    method:      T,
    output:      Option[Task]                = None,
    delegate:    Vector[Task]                = Vector.empty,
    environment: Option[EnvironmentBuilder] = None,
    grouping:    Option[Grouping]            = None,
    hooks:       Vector[Hook]                = Vector.empty,
    validate:    Validate                    = Validate.success)(implicit definitionScope: DefinitionScope): DSLContainer[T] =
    dsl match
      case dsl: DSLContainer[?] =>
        org.openmole.core.workflow.composition.DSLContainer[T](
          dsl,
          output orElse dsl.output,
          delegate ++ dsl.delegate,
          environment orElse dsl.environment,
          grouping orElse dsl.grouping,
          hooks ++ dsl.hooks,
          validate,
          method,
          definitionScope)
      case dsl =>
        org.openmole.core.workflow.composition.DSLContainer[T](
          dsl,
          output,
          delegate,
          environment,
          grouping,
          hooks,
          validate,
          method,
          definitionScope)

  export org.openmole.core.workflow.composition.DSLContainer
  export org.openmole.core.workflow.composition.DSLContainer.ExplorationMethod
  export org.openmole.core.workflow.composition.Hooked

  def Slot(dsl: DSL) = org.openmole.core.workflow.composition.Slot(dsl)
  def Capsule(node: DSL) = org.openmole.core.workflow.composition.Capsule(node)

  implicit def toOrigin[T](t: T)(using toOrigin: org.openmole.core.workflow.composition.DSL.ToOrigin[T]): TransitionOrigin = toOrigin.apply(t)
  implicit def toDestination[T](t: T)(using toDestination: org.openmole.core.workflow.composition.DSL.ToDestination[T]): TransitionDestination = toDestination.apply(t)
  implicit def toDestinationSeq[T](s: Seq[T])(using toDestination: org.openmole.core.workflow.composition.DSL.ToDestination[T]): Seq[TransitionDestination] = s.map(t => toDestination.apply(t))

  implicit def toNode[T](t: T)(using toNode: org.openmole.core.workflow.composition.DSL.ToTaskNode[T]): TaskNode = toNode.apply(t)
  implicit def toDSL[T](t: T)(using td: org.openmole.core.workflow.composition.DSL.ToDSL[T]): DSL = td.apply(t)
  implicit def toOptionalDSL[T](t: T)(using td: org.openmole.core.workflow.composition.DSL.ToDSL[T]): OptionalArgument[DSL] = OptionalArgument(toDSL(t))
  implicit def toMole[T](t: T)(using toMole: org.openmole.core.workflow.composition.DSL.ToMole[T]): Mole = toMole.apply(t)
  implicit def toMoleExecution[T](t: T)(using toMoleExecution: org.openmole.core.workflow.composition.DSL.ToMoleExecution[T]): MoleExecution = toMoleExecution.apply(t)

  implicit class DSLDecorator(t1: DSL):
    infix def &(t2: DSL) = new &(t1, t2)
    infix def and(t2: DSL) = new &(t1, t2)
  
    def outputs: Seq[Val[?]] = outputs(false)
    def outputs(explore: Boolean): Seq[Val[?]] =
      given DefinitionScope.InternalScope("outputs")
      val last = EmptyTask()
      val p = if !explore then DSL.toPuzzle(t1 -- last) else DSL.toPuzzle(t1 -< last)
      val mole = Puzzle.toMole(p)
      val slot = p.slots.toSeq.find(_.capsule._task == last).head
      TypeUtil.receivedTypes(mole, p.sources, p.hooks)(slot) toSeq
 
  /* ----------------- Patterns ------------------- */
 
  object Master:
    def apply(task: Task, persist: Val[?]*) = TaskNode(task, master = true, persist = persist)
    def apply(task: TaskNode, persist: Val[?]*) = task.copy(master = true, persist = persist)
 
  object Strain:
    def apply(task: Task) = TaskNode(task, strain = true)
    def apply(task: TaskNode) = task.copy(strain = true)
 
    def apply(dsl: DSL)(implicit scope: DefinitionScope = "strain"): DSL =
      val first = Strain(EmptyTask())
      val last = Strain(EmptyTask())
 
      val p = first -- dsl -- last
 
      val receivedFromDSL =
        val puzzle: Puzzle = DSL.toPuzzle(p)
        val mole = Puzzle.toMole(puzzle)
        TypeUtil.receivedTypes(mole, puzzle.sources, puzzle.hooks)(mole.slots(puzzle.lasts.head).head)
  
      p & (first oo last).block(receivedFromDSL.toSeq*)

  
  object Funnel:
    def apply(task: Task) = TaskNode(task, funnel = true)
    def apply(task: TaskNode) = task.copy(funnel = true)

}
