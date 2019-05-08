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

package org.openmole.core.workflow

/**
 * This package articulates workflows, by providing <code>Puzzle</code> and the bricks to construct them such as transitions.
 *
 * It also contains a large part of the DSL.
 */
package composition {

  import org.openmole.core.context.{ Context, Val }
  import org.openmole.core.expansion.Condition
  import org.openmole.core.outputmanager.OutputManager
  import org.openmole.core.workflow.builder.DefinitionScope
  import org.openmole.core.workflow.execution.{ EnvironmentProvider, LocalEnvironmentProvider }
  import org.openmole.core.workflow.mole.{ Grouping, Hook, MasterCapsule, Mole, MoleCapsule, MoleExecution, MoleExecutionContext, MoleServices, Source }
  import org.openmole.core.workflow.sampling.Sampling
  import org.openmole.core.workflow.task.{ EmptyTask, ExplorationTask, Task }
  import org.openmole.core.workflow.tools.OptionalArgument
  import org.openmole.core.workflow.transition._
  import org.openmole.core.workflow.validation.TypeUtil
  import shapeless.{ ::, HList }

  object Puzzle {

    def apply(s: TransitionSlot): Puzzle = Puzzle(s, lasts = Vector(s.capsule))

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
    def merge(p1: Puzzle, p2: Puzzle, mergeLasts: Boolean = false) = {
      new Puzzle(
        p1.firstSlot,
        if (!mergeLasts) p1.lasts else p1.lasts ++ p2.lasts,
        p1.transitions.toList ::: p2.transitions.toList,
        p1.dataChannels.toList ::: p2.dataChannels.toList,
        (p1.sources.toList ::: p2.sources.toList).distinct,
        (p1.hooks.toList ::: p2.hooks.toList).distinct,
        p1.environments ++ p2.environments,
        p1.grouping ++ p2.grouping
      )
    }

    def add(
      p:            Puzzle,
      transitions:  Iterable[ITransition]                 = Iterable.empty,
      dataChannels: Iterable[DataChannel]                 = Iterable.empty,
      sources:      Iterable[(MoleCapsule, Source)]       = Iterable.empty,
      hooks:        Iterable[(MoleCapsule, Hook)]         = Iterable.empty,
      environments: Map[MoleCapsule, EnvironmentProvider] = Map.empty,
      grouping:     Map[MoleCapsule, Grouping]            = Map.empty) =
      p.copy(
        transitions = p.transitions ++ transitions,
        dataChannels = p.dataChannels ++ dataChannels,
        sources = p.sources ++ sources,
        hooks = p.hooks ++ hooks,
        environments = p.environments ++ environments,
        grouping = p.grouping ++ grouping
      )

  }

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
  case class Puzzle(
    firstSlot:    TransitionSlot,
    lasts:        Iterable[MoleCapsule],
    transitions:  Iterable[ITransition]                 = Iterable.empty,
    dataChannels: Iterable[DataChannel]                 = Iterable.empty,
    sources:      Iterable[(MoleCapsule, Source)]       = Iterable.empty,
    hooks:        Iterable[(MoleCapsule, Hook)]         = Iterable.empty,
    environments: Map[MoleCapsule, EnvironmentProvider] = Map.empty,
    grouping:     Map[MoleCapsule, Grouping]            = Map.empty) {

    def toMole = new Mole(firstSlot.capsule, transitions, dataChannels)

    def toExecution(implicit moleServices: MoleServices): MoleExecution =
      MoleExecution(toMole, sources, hooks, environments, grouping)

    def toExecution(
      implicits:          Context                                    = Context.empty,
      seed:               OptionalArgument[Long]                     = None,
      executionContext:   OptionalArgument[MoleExecutionContext]     = None,
      defaultEnvironment: OptionalArgument[LocalEnvironmentProvider] = None)(implicit moleServices: MoleServices): MoleExecution =
      MoleExecution(
        mole = toMole,
        sources = sources,
        hooks = hooks,
        environments = environments,
        grouping = grouping,
        implicits = implicits,
        defaultEnvironment = defaultEnvironment,
        executionContext = executionContext
      )

    def slots: Set[TransitionSlot] = (firstSlot :: transitions.map(_.end).toList).toSet
    def first = firstSlot.capsule
    def inputs = first.inputs(toMole, sources, hooks).toSeq
    def defaults = first.task.defaults
  }

  case class TaskNode(task: Task, strain: Boolean = false, master: Boolean = false, persist: Seq[Val[_]] = Seq.empty, environment: Option[EnvironmentProvider] = None, grouping: Option[Grouping] = None, hooks: Vector[Hook] = Vector.empty, sources: Vector[Source] = Vector.empty) {
    def on(environment: EnvironmentProvider) = copy(environment = Some(environment))
    def by(strategy: Grouping) = copy(grouping = Some(strategy))
    def hook(hooks: Hook*) = copy(hooks = this.hooks ++ hooks)
    def source(sources: Source*) = copy(sources = this.sources ++ sources)
  }

  object TransitionOrigin {

    def tasks(d: TransitionOrigin) =
      d match {
        case TaskOrigin(n)          ⇒ Vector(n)
        case TransitionDSLOrigin(o) ⇒ DSL.tasks(o)
        case _                      ⇒ Vector.empty
      }

    def dsl(o: TransitionOrigin) =
      o match {
        case TransitionDSLOrigin(d) ⇒ Some(d)
        case _                      ⇒ None
      }
  }

  sealed trait TransitionOrigin {
    def --(d1: TransitionDestination, d: TransitionDestination*) = new --(this, (Seq(d1) ++ d).toVector)
    def --(d: Seq[TransitionDestination]) = new --(this, d.toVector)

    def -<(d: TransitionDestination*) = new -<(this, d.toVector)
    def >-(d: TransitionDestination*) = new >-(this, d.toVector)
    def >|(d: TransitionDestination*) = new >|(this, d.toVector)
    def -<-(d: TransitionDestination*) = new -<-(this, d.toVector)

    def oo(d: TransitionDestination*) = new oo(this, d.toVector)
  }

  case class TaskOrigin(node: TaskNode) extends TransitionOrigin
  case class TransitionDSLOrigin(t: DSL) extends TransitionOrigin

  object TransitionDestination {

    def tasks(d: TransitionDestination) =
      d match {
        case TaskDestination(n)          ⇒ Vector(n)
        case TransitionDSLDestination(t) ⇒ DSL.tasks(t)
        case _                           ⇒ Vector.empty
      }

    def dsl(o: TransitionDestination) =
      o match {
        case TransitionDSLDestination(d) ⇒ Some(d)
        case _                           ⇒ None
      }

  }

  sealed trait TransitionDestination
  case class TaskDestination(node: TaskNode) extends TransitionDestination
  case class TransitionDSLDestination(t: DSL) extends TransitionDestination

  object DSL {

    def tasks(t: DSL): Vector[TaskNode] = t match {
      case --(o, d, _, _)     ⇒ TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case -<(o, d, _, _)     ⇒ TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case >-(o, d, _, _)     ⇒ TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case >|(o, d, _, _)     ⇒ TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case -<-(o, d, _, _, _) ⇒ TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case oo(o, d, _)        ⇒ TransitionOrigin.tasks(o) ++ d.flatMap(TransitionDestination.tasks)
      case &(a, b)            ⇒ tasks(a) ++ tasks(b)
      case Slot(d)            ⇒ tasks(d)
      case Capsule(d, _)      ⇒ Vector(d)
      case c: DSLContainer[_] ⇒ DSLContainer.taskNodes(c) ++ tasks(c.dsl)
      case TaskNodeDSL(n)     ⇒ Vector(n)
    }

  }

  /* -------------------- Transition DSL ---------------------- */
  sealed trait DSL

  case class --(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL {
    def when(condition: Condition) = copy(condition = condition)
    def filter(filter: BlockList) = copy(filterValue = filter)
    def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
    def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))
  }

  case class -<(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL {
    def when(condition: Condition) = copy(condition = condition)
    def filter(filter: BlockList) = copy(filterValue = filter)
    def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
    def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))
  }

  case class >-(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL {
    def when(condition: Condition) = copy(condition = condition)
    def filter(filter: BlockList) = copy(filterValue = filter)
    def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
    def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))
  }

  case class >|(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty) extends DSL {
    def when(condition: Condition) = copy(condition = condition)
    def filter(filter: BlockList) = copy(filterValue = filter)
    def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
    def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))
  }

  case class -<-(a: TransitionOrigin, b: Vector[TransitionDestination], condition: Condition = Condition.True, filterValue: BlockList = BlockList.empty, slaves: Option[Int] = None) extends DSL {
    def when(condition: Condition) = copy(condition = condition)
    def filter(filter: BlockList) = copy(filterValue = filter)
    def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
    def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))
    def slaves(n: Int) = copy(slaves = Some(n))
    def slaves(n: Option[Int]) = copy(slaves = n)
  }

  case class oo(a: TransitionOrigin, b: Vector[TransitionDestination], filterValue: BlockList = BlockList.empty) extends DSL {
    def filter(filter: BlockList) = copy(filterValue = filter)
    def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
    def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))
  }

  case class &(a: DSL, b: DSL) extends DSL

  object DSLContainer {
    def taskNodes(container: DSLContainer[_]) = {
      val output = container.output.map { o ⇒ TaskNode(o, hooks = container.hooks) }
      val delegate = container.delegate.map { t ⇒ TaskNode(t, environment = container.environment, grouping = container.grouping) }
      delegate ++ output
    }
  }

  case class DSLContainer[+T](
    dsl:         DSL,
    output:      Option[Task]                = None,
    delegate:    Vector[Task]                = Vector.empty,
    environment: Option[EnvironmentProvider] = None,
    grouping:    Option[Grouping]            = None,
    hooks:       Vector[Hook]                = Vector.empty,
    data:        T,
    scope:       DefinitionScope) extends DSL {
    def on(environment: EnvironmentProvider) = copy(environment = Some(environment))
    def by(strategy: Grouping) = copy(grouping = Some(strategy))
  }

  case class TaskNodeDSL(node: TaskNode) extends DSL
  case class Slot(dsl: DSL) extends DSL
  case class Capsule(node: TaskNode, id: Any = new Object) extends DSL

  trait CompositionPackage {

    type DSL = composition.DSL
    val DSL = composition.DSL

    class DSLContainerHook[T](dsl: DSLContainer[T]) {
      def hook(hooks: Hook*) = dsl.copy(hooks = dsl.hooks ++ hooks)
    }

    implicit def hookDecorator[T](container: DSLContainer[T]) = new DSLContainerHook(container)

    def DSLContainer(
      transitionDSL: DSL,
      output:        Option[Task]                = None,
      delegate:      Vector[Task]                = Vector.empty,
      environment:   Option[EnvironmentProvider] = None,
      grouping:      Option[Grouping]            = None,
      hooks:         Vector[Hook]                = Vector.empty)(implicit definitionScope: DefinitionScope) =
      composition.DSLContainer[Unit](
        transitionDSL,
        output,
        delegate,
        environment,
        grouping,
        hooks,
        Unit,
        definitionScope
      )

    def DSLContainerExtension[T](
      dsl:         DSLContainer[_],
      data:        T,
      output:      Option[Task]                = None,
      delegate:    Vector[Task]                = Vector.empty,
      environment: Option[EnvironmentProvider] = None,
      grouping:    Option[Grouping]            = None,
      hooks:       Vector[Hook]                = Vector.empty)(implicit definitionScope: DefinitionScope) =
      composition.DSLContainer[T](
        dsl,
        output orElse dsl.output,
        delegate ++ dsl.delegate,
        environment orElse dsl.environment,
        grouping orElse dsl.grouping,
        hooks ++ dsl.hooks,
        data,
        definitionScope)

    type DSLContainer[T] = composition.DSLContainer[T]

    def Slot(dsl: DSL) = composition.Slot(dsl)
    def Capsule(node: TaskNode) = composition.Capsule(node)

    /* ---------- Transition DSL to Puzzle -----------------*/

    def dslToPuzzle(t: DSL): Puzzle = {
      val taskNodeList = DSL.tasks(t)
      val taskEnvironment = taskNodeList.groupBy(n ⇒ n.task).mapValues(_.flatMap(_.environment).headOption)
      val taskGrouping = taskNodeList.groupBy(n ⇒ n.task).mapValues(_.flatMap(_.grouping).headOption)

      def taskToSlot(dsl: DSL) = {
        def buildCapsule(task: Task, ns: Vector[TaskNode]) = {
          val strain = ns.exists(_.strain)
          val master = ns.exists(_.master)

          if (!master) MoleCapsule(task, strain = strain)
          else {
            val persist = ns.find(_.master).get.persist
            MasterCapsule(task, persist = persist, strain = strain)
          }
        }

        DSL.tasks(dsl).groupBy(_.task).map { case (t, ns) ⇒ t -> TransitionSlot(buildCapsule(t, ns)) }
      }

      def taskNodeToPuzzle(n: TaskNode, slots: Map[Task, TransitionSlot]) = {
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
      }

      def transitionDSLToPuzzle0(t: DSL, slots: Map[Task, TransitionSlot], converted: collection.mutable.Map[DSL, Puzzle]): Puzzle = {
        def transitionOriginToPuzzle(d: TransitionOrigin): Puzzle =
          d match {
            case TaskOrigin(n)          ⇒ taskNodeToPuzzle(n, slots)
            case TransitionDSLOrigin(o) ⇒ transitionDSLToPuzzle0(o, slots, converted)
          }

        def transitionDestinationToPuzzle(d: TransitionDestination): (Puzzle, Boolean) =
          d match {
            case TaskDestination(n)                ⇒ taskNodeToPuzzle(n, slots) -> false
            case TransitionDSLDestination(t: Slot) ⇒ transitionDSLToPuzzle0(t, slots, converted) -> true
            case TransitionDSLDestination(t)       ⇒ transitionDSLToPuzzle0(t, slots, converted) -> false
          }

        def transitionsToPuzzle[T](o: TransitionOrigin, d: Vector[TransitionDestination], add: (Puzzle, Iterable[T]) ⇒ Puzzle)(transition: (MoleCapsule, TransitionSlot) ⇒ T) = {
          val originPuzzle = transitionOriginToPuzzle(o)
          val destinationPuzzle = d.map(transitionDestinationToPuzzle)

          val transitions =
            for {
              l ← originPuzzle.lasts
              (f, newSlot) ← destinationPuzzle
            } yield transition(l, if (!newSlot) f.firstSlot else TransitionSlot(f.first))

          val merged = destinationPuzzle.unzip._1.foldLeft(originPuzzle) {
            case (a, b) ⇒ Puzzle.merge(a, b)
          }

          val plugged = merged.copy(firstSlot = originPuzzle.firstSlot, lasts = destinationPuzzle.unzip._1.flatMap(_.lasts))
          add(plugged, transitions)

        }

        def dslContainerToPuzzle(container: DSLContainer[_]) = {
          val puzzle = transitionDSLToPuzzle0(container.dsl, slots, converted)
          def outputs = container.output.map(t ⇒ Vector(slots(t).capsule)).getOrElse(puzzle.lasts)

          val hooks =
            for {
              o ← outputs
              h ← container.hooks
            } yield o -> h

          val environments =
            for {
              c ← container.delegate.map(d ⇒ slots(d).capsule)
              e ← container.environment
            } yield c -> e

          Puzzle.add(
            puzzle,
            hooks = hooks,
            environments = environments.toMap
          )
        }

        def addTransitions(p: Puzzle, transitions: Iterable[ITransition]) = Puzzle.add(p, transitions = transitions)
        def addDataChannel(p: Puzzle, channels: Iterable[DataChannel]) = Puzzle.add(p, dataChannels = channels)

        def p =
          t match {
            case --(o, d, condition, filter)          ⇒ transitionsToPuzzle(o, d, addTransitions) { case (c, s) ⇒ new Transition(c, s, condition, filter) }
            case -<(o, d, condition, filter)          ⇒ transitionsToPuzzle(o, d, addTransitions) { case (c, s) ⇒ new ExplorationTransition(c, s, condition, filter) }
            case >-(o, d, condition, filter)          ⇒ transitionsToPuzzle(o, d, addTransitions) { case (c, s) ⇒ new AggregationTransition(c, s, condition, filter) }
            case >|(o, d, condition, filter)          ⇒ transitionsToPuzzle(o, d, addTransitions) { case (c, s) ⇒ new EndExplorationTransition(c, s, condition, filter) }
            case -<-(o, d, condition, filter, slaves) ⇒ transitionsToPuzzle(o, d, addTransitions) { case (c, s) ⇒ new SlaveTransition(c, s, condition, filter, slaves = slaves) }
            case oo(o, d, filter)                     ⇒ transitionsToPuzzle(o, d, addDataChannel) { case (c, s) ⇒ DataChannel(c, s, filter) }
            case &(a, b)                              ⇒ Puzzle.merge(transitionDSLToPuzzle0(a, slots, converted), transitionDSLToPuzzle0(b, slots, converted))
            case c: DSLContainer[_]                   ⇒ dslContainerToPuzzle(c)
            case TaskNodeDSL(n)                       ⇒ taskNodeToPuzzle(n, slots)
            case c @ Capsule(n, _)                    ⇒ taskNodeToPuzzle(n, taskToSlot(c))
            case Slot(d)                              ⇒ transitionDSLToPuzzle0(d, slots, converted)
          }

        converted.getOrElseUpdate(t, p)

      }

      transitionDSLToPuzzle0(t, taskToSlot(t), collection.mutable.Map())
    }

    object DSLSelector {
      def apply[L <: HList](implicit selector: DSLSelector[L]): DSLSelector[L] = selector

      implicit def select[T <: HList]: DSLSelector[DSL :: T] =
        new DSLSelector[DSL :: T] {
          def apply(l: DSL :: T) = l.head
        }

      implicit def recurse[H, T <: HList](implicit st: DSLSelector[T]): DSLSelector[H :: T] =
        new DSLSelector[H :: T] {
          def apply(l: H :: T) = st(l.tail)
        }
    }

    trait DSLSelector[-L <: HList] {
      def apply(l: L): DSL
    }

    /* ----------- implicit conversions ----------------- */

    object ToOrigin {
      def apply[T](f: T ⇒ TransitionOrigin) = new ToOrigin[T] {
        def apply(t: T) = f(t)
      }

      implicit def taskToOrigin = ToOrigin[Task] { t ⇒ TaskOrigin(TaskNode(t)) }
      implicit def transitionDSLToOrigin = ToOrigin[DSL] { TransitionDSLOrigin(_) }
      implicit def taskTransitionPieceToOrigin = ToOrigin[TaskNode] { n ⇒ TaskOrigin(n) }
      implicit def samplingToOrigin(implicit scope: DefinitionScope) = ToOrigin[Sampling] { s ⇒ taskToOrigin(ExplorationTask(s)) }
    }

    trait ToOrigin[-T] {
      def apply(t: T): TransitionOrigin
    }

    object ToDestination {
      def apply[T](f: T ⇒ TransitionDestination) = new ToDestination[T] {
        def apply(t: T) = f(t)
      }

      implicit def taskToDestination = ToDestination[Task] { t ⇒ TaskDestination(TaskNode(t)) }
      implicit def taskTransitionPieceToDestination = ToDestination[TaskNode] { t ⇒ TaskDestination(t) }
      implicit def samplingToDestination(implicit scope: DefinitionScope) = ToDestination[Sampling] { s ⇒ taskToDestination(ExplorationTask(s)) }
      implicit def transitionDSLToDestination = ToDestination[DSL] { TransitionDSLDestination(_) }
    }

    trait ToDestination[-T] {
      def apply(t: T): TransitionDestination
    }

    object ToNode {
      def apply[T](f: T ⇒ TaskNode) = new ToNode[T] {
        def apply(t: T) = f(t)
      }

      implicit def taskToTransitionPiece = apply[Task](t ⇒ TaskNode(t))
    }

    trait ToNode[-T] {
      def apply(t: T): TaskNode
    }

    object ToDSL {
      def apply[T](f: T ⇒ DSL) = new ToDSL[T] {
        def apply(t: T) = f(t)
      }

      implicit def dslToDSL = ToDSL[DSL](identity)
      implicit def taskToTransitionDSL = ToDSL[Task](t ⇒ TaskNodeDSL(TaskNode(t)))
      implicit def taskInNodeToTransitionDSL = ToDSL[TaskNode](t ⇒ TaskNodeDSL(t))
      implicit def transitionDSLSelectorToTransitionDSL[HL <: HList](implicit dslSelector: DSLSelector[HL]) = ToDSL[HL](t ⇒ dslSelector(t))
    }

    trait ToDSL[-T] {
      def apply(t: T): DSL
    }

    object ToMole {
      def apply[T](f: T ⇒ Mole) = new ToMole[T] {
        def apply(t: T) = f(t)
      }

      implicit def transitionDSLToMole = ToMole[DSL](t ⇒ dslToPuzzle(t).toMole)
      implicit def toTransitionDSLToMoleExecution[T: ToDSL] = ToMole[T] { t ⇒ transitionDSLToMole.apply(implicitly[ToDSL[T]].apply(t)) }
    }

    trait ToMole[-T] {
      def apply(t: T): Mole
    }

    object ToMoleExecution {
      def apply[T](f: T ⇒ MoleExecution) = new ToMoleExecution[T] {
        def apply(t: T) = f(t)
      }

      implicit def toDSLToMoleExecution[T: ToDSL](implicit moleServices: MoleServices) = ToMoleExecution[T] { t ⇒ dslToPuzzle(implicitly[ToDSL[T]].apply(t)).toExecution }
    }

    trait ToMoleExecution[-T] {
      def apply(t: T): MoleExecution
    }

    implicit def toOrigin[T: ToOrigin](t: T) = implicitly[ToOrigin[T]].apply(t)
    implicit def toDestination[T: ToDestination](t: T) = implicitly[ToDestination[T]].apply(t)
    implicit def toDestinationSeq[T: ToDestination](s: Seq[T]) = s.map(t ⇒ implicitly[ToDestination[T]].apply(t))

    implicit def toNode[T](t: T)(implicit toNode: ToNode[T]) = toNode.apply(t)
    implicit def toDSL[T: ToDSL](t: T) = implicitly[ToDSL[T]].apply(t)
    implicit def toOptionalDSL[T: ToDSL](t: T) = OptionalArgument(Some(toDSL(t)))
    implicit def toMole[T: ToMole](t: T) = implicitly[ToMole[T]].apply(t)
    implicit def toMoleExecution[T: ToMoleExecution](t: T) = implicitly[ToMoleExecution[T]].apply(t)

    implicit class DSLDecorator(t1: DSL) {
      def &(t2: DSL) = new &(t1, t2)

      def and(t2: DSL) = new &(t1, t2)

      def outputs: Seq[Val[_]] = outputs(false)
      def outputs(explore: Boolean): Seq[Val[_]] = {
        implicit def scope = DefinitionScope.Internal("outputs")
        val last = EmptyTask()
        val p: Puzzle = if (!explore) dslToPuzzle(t1 -- last) else dslToPuzzle(t1 -< last)
        val mole = p.toMole
        val slot = p.slots.toSeq.find(_.capsule.task == last).head
        TypeUtil.receivedTypes(mole, p.sources, p.hooks)(slot) toSeq
      }
    }

    /* ----------------- Patterns ------------------- */

    object Master {
      def apply(task: Task, persist: Val[_]*) = TaskNode(task, master = true, persist = persist)
      def apply(task: TaskNode, persist: Val[_]*) = task.copy(master = true, persist = persist)
    }

    object Strain {

      def apply(task: Task) = TaskNode(task, strain = true)

      def apply(task: TaskNode) = task.copy(strain = true)

      def apply(dsl: DSL)(implicit scope: DefinitionScope = "strain"): DSL = {
        val first = Strain(EmptyTask())
        val last = Strain(EmptyTask())

        val p = first -- dsl -- last

        val receivedFromDSL = {
          val puzzle: Puzzle = dslToPuzzle(p)
          val mole = puzzle.toMole
          TypeUtil.receivedTypes(mole, puzzle.sources, puzzle.hooks)(mole.slots(puzzle.lasts.head).head)
        }

        p & (first oo last block (receivedFromDSL.toSeq: _*))
      }

    }

  }

}
