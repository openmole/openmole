/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.validation

import org.openmole.core.context.*
import org.openmole.core.argument.{Default, DefaultSet}
import org.openmole.core.fileservice.FileService
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.composition.Puzzle
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.task.*
import org.openmole.core.workflow.validation.DataflowProblem.*
import org.openmole.core.workflow.validation.TopologyProblem.*
import org.openmole.core.workflow.validation.TypeUtil.*
import org.openmole.core.workflow.validation.ValidationProblem.{HookValidationProblem, MoleValidationProblem, SourceValidationProblem, TaskValidationProblem, TransitionValidationProblem}
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.outputredirection.OutputRedirection

import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.collection.mutable.{HashMap, Queue}
import scala.util.{Failure, Success, Try}

object Validation {

  def allMoles(mole: Mole, sources: Sources, hooks: Hooks, in: Option[(MoleTask, MoleCapsule)] = None): List[(Mole, Option[(MoleTask, MoleCapsule)])] =
    (mole, in) ::
      mole.capsules.flatMap: c =>
        c.task(mole, sources, hooks) match
          case mt: MoleTask => allMoles(mt.mole, sources, hooks, Some(mt -> c))
          case _            => List.empty
      .toList


  private def prototypesToMap(prototypes: Iterable[Val[?]]) = prototypes.map(i => i.name -> i).toMap

  private def defaultMaps(p: DefaultSet) =
    val (po, pno) = p.partition(_.`override`)

    (
      prototypesToMap(po.map(_.prototype) ++ Task.openMOLEDefault),
      prototypesToMap(pno.map(_.prototype))
    )

  abstract class errorDetect(mole: Mole, implicits: Iterable[Val[?]], sources: Sources, hooks: Hooks):
    def checkPrototypeMatch(p: Val[?]): Problem
    val implicitMap = prototypesToMap(implicits)

  def taskTypeErrors(mole: Mole)(capsules: Iterable[MoleCapsule], implicits: Iterable[Val[?]], sources: Sources, hooks: Hooks) = {

    val implicitMap = prototypesToMap(implicits)

    (for {
      c <- capsules
      sourcesOutputs = TreeMap(sources(c).flatMap((os: Source) => os.outputs.toSet).map(o => o.name -> o).toSeq *)
      s <- mole.slots(c)
      computedTypes = TypeUtil.validTypes(mole, sources, hooks)(s)
      receivedInputs = TreeMap(computedTypes.map { p => p.name -> p }.toSeq *)
      (defaultsOverride, defaultsNonOverride) = defaultMaps(Task.defaults(c.task(mole, sources, hooks)))
      input <- Task.inputs(c.task(mole, sources, hooks))
    } yield {
      def checkPrototypeMatch(p: Val[?]) =
        if (!input.isAssignableFrom(p)) Some(WrongType(s, input, p))
        else None

      val inputName = input.name

      val defaultOverride = defaultsOverride.get(inputName)
      val receivedInput = receivedInputs.get(inputName)
      val receivedSource = sourcesOutputs.get(inputName)
      val receivedImplicit = implicitMap.get(inputName)
      val defaultNonOverride = defaultsNonOverride.get(inputName)

      (defaultOverride, receivedInput, receivedSource, receivedImplicit, defaultNonOverride) match {
        case (Some(parameter), _, _, _, _)               => checkPrototypeMatch(parameter)
        case (None, Some(received), impl, source, param) => checkPrototypeMatch(received.toVal)
        case (None, None, Some(source), impl, param)     => checkPrototypeMatch(source)
        case (None, None, None, Some(impl), _)           => checkPrototypeMatch(impl)
        case (None, None, None, None, Some(parameter))   => checkPrototypeMatch(parameter)
        case (None, None, None, None, None) =>
          val inputs = mutable.TreeSet[Val[?]]()
          inputs ++= defaultNonOverride
          inputs ++= receivedImplicit
          inputs ++= receivedInputs.map(_._2.toVal)
          inputs ++= defaultOverride
          Some(MissingInput(s, input, inputs.toSeq))
      }
    }).flatten
  }

  def taskValidationErrors(mole: Mole, sources: Sources, hooks: Hooks)(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache, outputRedirection: OutputRedirection) =
    def taskValidates = mole.capsules.map(_.task(mole, sources, hooks)).collect { case v: ValidateTask => v }

    taskValidates.flatMap: t =>
      t.validate(Task.inputs(t).toSeq).toList match
        case Nil => None
        case e   => Some(TaskValidationProblem(t, e))

  def sourceErrors(mole: Mole, implicits: Iterable[Val[?]], sources: Sources, hooks: Hooks)(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache, outputRedirection: OutputRedirection) =
    val implicitMap = prototypesToMap(implicits)

    def inputErrors =
      for {
        c <- mole.capsules
        (so: Source) <- sources.getOrElse(c, List.empty)
        (defaultsOverride, defaultsNonOverride) = defaultMaps(so.defaults)
        sl <- mole.slots(c)
        receivedInputs = TreeMap(TypeUtil.validTypes(mole, sources, hooks)(sl).map { p => p.name -> p }.toSeq *)
        i <- so.inputs
      } yield {
        def checkPrototypeMatch(p: Val[?]) =
          if (!i.isAssignableFrom(p)) Some(WrongSourceType(sl, so, i, p))
          else None

        val inputName = i.name

        val defaultOverride = defaultsOverride.get(inputName)
        val receivedInput = receivedInputs.get(inputName)
        val receivedImplicit = implicitMap.get(inputName)
        val defaultNonOverride = defaultsNonOverride.get(inputName)

        (defaultOverride, receivedInput, receivedImplicit, defaultNonOverride) match {
          case (Some(parameter), _, _, _)          => checkPrototypeMatch(parameter)
          case (None, Some(received), impl, param) => checkPrototypeMatch(received.toVal)
          case (None, None, Some(impl), _)         => checkPrototypeMatch(impl)
          case (None, None, None, Some(param))     => checkPrototypeMatch(param)
          case (None, None, None, None)            => Some(MissingSourceInput(sl, so, i))
        }
      }

    def validationErrors =
      for {
        c <- mole.capsules
        source <- sources.getOrElse(c, List.empty).collect { case s: ValidateSource => s }
        (defaultsOverride, defaultsNonOverride) = defaultMaps(source.defaults)
        sl <- mole.slots(c)
        receivedInputs = TreeMap(TypeUtil.validTypes(mole, sources, hooks)(sl).map { p => p.name -> p }.toSeq *).mapValues(_.toVal)
      } yield {
        val inputs = (defaultsNonOverride ++ implicitMap ++ receivedInputs ++ defaultsOverride).toSeq.map(_._2)

        source.validate(inputs).toList match {
          case Nil => None
          case e   => Some(SourceValidationProblem(source, e))
        }
      }

    inputErrors.flatten ++ validationErrors.flatten

  def typeErrorsTopMole(mole: Mole, implicits: Iterable[Val[?]], sources: Sources, hooks: Hooks) =
    taskTypeErrors(mole)(mole.capsules, implicits, sources, hooks)

  def typeErrorsMoleTask(mole: Mole, implicits: Iterable[Val[?]]) =
    taskTypeErrors(mole)(mole.capsules.filterNot(_ == mole.root), implicits, Sources.empty, Hooks.empty)

  def topologyErrors(mole: Mole) =
    val seen = new HashMap[MoleCapsule, (List[(List[MoleCapsule], Int)])]
    val toProcess = new Queue[(MoleCapsule, Int, List[MoleCapsule])]

    toProcess.enqueue((mole.root, 0, List.empty))
    seen(mole.root) = List((List.empty -> 0))

    while (!toProcess.isEmpty) {
      val (capsule, level, path) = toProcess.dequeue

      Mole.nextCapsules(mole)(capsule, level).foreach {
        case (nCap, nLvl) =>
          if (!seen.contains(nCap)) toProcess.enqueue((nCap, nLvl, capsule :: path))
          seen(nCap) = ((capsule :: path) -> nLvl) :: seen.getOrElse(nCap, List.empty)
      }
    }

    seen.filter { case (caps, paths) => paths.map { case (path, level) => level }.distinct.size > 1 }.map {
      case (caps, paths) => LevelProblem(caps, paths)
    } ++
      seen.flatMap {
        case (caps, paths) =>
          paths.filter { case (_, level) => level < 0 }.map { case (path, level) => NegativeLevelProblem(caps, path.reverse, level) }
      } ++ (mole.transitions.map(_.start).toSet -- seen.keys).toVector.map { capsule => UnreachableCapsuleProblem(capsule) }

  def moleTaskTopologyError(moleTask: MoleTask, capsule: MoleCapsule) =
    moleTask.mole.level(moleTask.last) match
      case 0 => List()
      case l => List(MoleTaskLastCapsuleProblem(capsule, moleTask, l))

  def duplicatedTransitions(mole: Mole) =
    for
      end <- mole.capsules
      slot <- mole.slots(end)
      (_, transitions) <- mole.inputTransitions(slot).toList.map { t => t.start -> t }.groupBy { case (c, _) => c }
      if (transitions.size > 1)
    yield
      DuplicatedTransition(transitions.unzip._2)

  def duplicatedName(mole: Mole, sources: Sources, hooks: Hooks) =
    def duplicated(data: PrototypeSet) =
      data.prototypes.groupBy(_.name).filter { case (_, d) => d.map(_.`type`).distinct.size > 1 }

    mole.capsules.flatMap: c =>
      duplicated(c.inputs(mole, sources, hooks)).map { (name, data) => DuplicatedName(c, name, data, SlotType.Input) } ++
        duplicated(c.outputs(mole, sources, hooks)).map { (name, data) => DuplicatedName(c, name, data, SlotType.Output) }


  def transitionValidationErrors(mole: Mole, sources: Sources, hooks: Hooks)(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache, outputRedirection: OutputRedirection) =
    val errors =
      for {
        transition <- mole.transitions.collect { case x: ValidateTransition => x }
      } yield {
        val inputs = TypeUtil.validTypes(mole, sources, hooks)(transition.end, _ == transition)
        transition.validate(inputs.toSeq.map(_.toVal)) match {
          case ts if !ts.isEmpty => Some(TransitionValidationProblem(transition, ts))
          case _                 => None
        }
      }

    errors.flatten

  def incoherentTypeAggregation(mole: Mole, sources: Sources, hooks: Hooks) =
    for {
      c <- mole.capsules
      inputs = c.inputs(mole, sources, hooks)
      slot <- mole.slots(c)
      invalidType <- TypeUtil.computeTypes(mole, sources, hooks)(slot).collect { case x: InvalidType => x }
      if inputs.contains(invalidType.name)
    } yield IncoherentTypeAggregation(slot, invalidType)

  def incoherentTypeBetweenSlots(mole: Mole, sources: Sources, hooks: Hooks) =
    (for {
      c <- mole.capsules
      inputs = c.inputs(mole, sources, hooks)
    } yield {
      val slotsInputs = mole.slots(c).map { s => TypeUtil.validTypes(mole, sources, hooks)(s).toSeq }.flatten.groupBy(_.name).toSeq
      for {
        (name, ts) <- slotsInputs
        if inputs.contains(name)
        types = ts.toSeq.map(_.`type`)
        if types.distinct.size != 1
      } yield IncoherentTypesBetweenSlots(c, name, types)

    }).flatten

  private def moleTaskInputMaps(moleTask: MoleTask) =
    (moleTask.mole.root.inputs(moleTask.mole, Sources.empty, Hooks.empty).toList ++
      Task.inputs(moleTask)).map(i => i.name -> i).toMap[String, Val[?]]

  def moleTaskImplicitsErrors(moleTask: MoleTask, capsule: MoleCapsule) = {
    val inputs = moleTaskInputMaps(moleTask)
    moleTask.implicits.filterNot(inputs.contains).map(i => MissingMoleTaskImplicit(capsule, i))
  }

  def hookErrors(m: Mole, implicits: Iterable[Val[?]], sources: Sources, hooks: Hooks)(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache, outputRedirection: OutputRedirection): Iterable[Problem] =
    val implicitMap = prototypesToMap(implicits)

    def inputsErrors =
      for
        c <- m.capsules
        outputs = c.outputs(m, sources, Hooks.empty).toMap
        h <- hooks(c)
        (defaultsOverride, defaultsNonOverride) = defaultMaps(h.defaults)
        i <- h.inputs
      yield
        val inputName = i.name

        val defaultOverride = defaultsOverride.get(inputName)
        val receivedInput = outputs.get(inputName)
        val receivedImplicit = implicitMap.get(inputName)
        val defaultNonOverride = defaultsNonOverride.get(inputName)

        val computed =
          (defaultOverride, receivedInput, receivedImplicit, defaultNonOverride) match
            case (Some(parameter), _, _, _)      => Some(parameter)
            case (None, Some(received), _, _)    => Some(received)
            case (None, None, Some(impl), _)     => Some(impl)
            case (None, None, None, Some(param)) => Some(param)
            case (None, None, None, None)        => None

        def checkPrototypeMatch(p: Val[?]) =
          if (!i.isAssignableFrom(p)) Some(WrongHookType(c, h, i, p)) else None

        computed match
          case None    => Some(MissingHookInput(c, h, i))
          case Some(c) => checkPrototypeMatch(c)

    def validationErrors =
      for
        c <- m.capsules
        outputs = c.outputs(m, sources, Hooks.empty).toMap
        h <- hooks(c).collect { case v: ValidateHook => v }
        (defaultsOverride, defaultsNonOverride) = defaultMaps(h.defaults)
      yield
        val inputs = (defaultsNonOverride ++ implicitMap ++ outputs ++ defaultsOverride).toSeq.map(_._2)
        h.validate(inputs).toList match
          case Nil => None
          case e   => Some(HookValidationProblem(h, e))

    inputsErrors.flatten ++ validationErrors.flatten

  def dataChannelErrors(mole: Mole) =
    val noTransitionProblems =
      mole.dataChannels.flatMap { dc => List(dc -> dc.start, dc -> dc.end.capsule) }.flatMap {
        case (dc, capsule) =>
          Try(mole.level(capsule)) match {
            case Success(_) => None
            case Failure(_) => Some(NoTransitionToCapsuleProblem(capsule, dc))
          }
      }

    val dataChannelWithProblem = noTransitionProblems.map(_.dataChannel).toSet

    val negativeLevelProblem =
      mole.dataChannels.filter(dc => !dataChannelWithProblem.contains(dc)).filter {
        dc => mole.level(dc.end.capsule) < mole.level(dc.start)
      }.map(DataChannelNegativeLevelProblem(_))

    noTransitionProblems ++ negativeLevelProblem

  def moleValidateErrors(mole: Mole)(implicit newFile: TmpDirectory, fileService: FileService, cache: KeyValueCache, outputRedirection: OutputRedirection) =
    mole.validate(Seq.empty) match
      case s if !s.isEmpty => Seq(MoleValidationProblem(mole, s))
      case _               => Seq()


  def apply(dsl: org.openmole.core.workflow.composition.DSL)(implicit tmpDirectory: TmpDirectory, fileService: FileService, cache: KeyValueCache, outputRedirection: OutputRedirection): List[Problem] = 
    import org.openmole.core.workflow.dsl._
    val puzzle = DSL.toPuzzle(dsl)
    apply(
      mole = Puzzle.toMole(puzzle),
      sources = puzzle.sources,
      hooks = puzzle.hooks
    )

  def apply(mole: Mole, implicits: Context = Context.empty, sources: Sources = Sources.empty, hooks: Hooks = Hooks.empty)(using TmpDirectory, FileService, KeyValueCache, OutputRedirection): List[Problem] =
    allMoles(mole, sources, hooks).flatMap:
      case (m, mt) =>
        def moleTaskImplicits(moleTask: MoleTask) =
          val inputs = moleTaskInputMaps(moleTask)
          moleTask.implicits.flatMap(i => inputs.get(i))

        def sourceHookOrMtError =
          mt match
            case Some((t, c)) =>
              moleTaskImplicitsErrors(t, c) ++
                typeErrorsMoleTask(m, moleTaskImplicits(t)).map { e => MoleTaskDataFlowProblem(c, e) } ++
                moleTaskTopologyError(t, c)
            case None =>
              sourceErrors(m, implicits.prototypes, sources, hooks) ++
                hookErrors(m, implicits.prototypes, sources, hooks) ++
                typeErrorsTopMole(m, implicits.prototypes, sources, hooks)

        sourceHookOrMtError ++
          topologyErrors(m) ++
          duplicatedTransitions(m) ++
          duplicatedName(m, sources, hooks) ++
          dataChannelErrors(m) ++
          incoherentTypeAggregation(m, sources, hooks) ++
          incoherentTypeBetweenSlots(m, sources, hooks) ++
          taskValidationErrors(m, sources, hooks) ++
          transitionValidationErrors(m, sources, hooks) ++
          moleValidateErrors(m)


}
