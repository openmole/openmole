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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation.DataflowProblem._
import org.openmole.core.workflow.validation.TopologyProblem._
import org.openmole.core.workflow.validation.TypeUtil.computeManifests

import scala.collection.immutable.TreeMap
import scala.collection.mutable.{ HashMap, Queue }

object Validation {

  def allMoles(mole: Mole) =
    (mole, None) ::
      mole.capsules.flatMap(
        c ⇒
          c.task match {
            case mt: MoleTask ⇒ Some(mt.mole -> Some(mt -> c))
            case _            ⇒ None
          }
      ).toList

  private def paramsToMap(params: Iterable[Default[_]]) =
    params.map {
      p ⇒ p.prototype.name -> p.prototype
    }.toMap[String, Prototype[_]]

  private def prototypesToMap(prototypes: Iterable[Prototype[_]]) = prototypes.map { i ⇒ i.name -> i }.toMap[String, Prototype[_]]

  private def separateDefaults(p: DefaultSet) = {
    val (po, pno) = p.partition(_.`override`)
    (paramsToMap(po), paramsToMap(pno))
  }

  abstract class errorDetect(mole: Mole, implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks) {
    def checkPrototypeMatch(p: Prototype[_]): Problem

    val implicitMap = prototypesToMap(implicits)

  }

  def taskTypeErrors(mole: Mole)(capsules: Iterable[Capsule], implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks) = {

    val implicitMap = prototypesToMap(implicits)

    (for {
      (c: Capsule) ← capsules
      sourcesOutputs = TreeMap(sources(c).flatMap((os: Source) ⇒ os.outputs.toSet).map((o: Data[_]) ⇒ o.prototype.name -> o).toSeq: _*)
      s ← mole.slots(c)
      receivedInputs = TreeMap(computeManifests(mole, sources, hooks)(s).map { p ⇒ p.name -> p }.toSeq: _*)
      (defaultsOverride, defaultsNonOverride) = separateDefaults(c.task.defaults)
      input ← c.task.inputs
    } yield {
      def checkPrototypeMatch(p: Prototype[_]) =
        if (!input.prototype.isAssignableFrom(p)) Some(WrongType(s, input, p))
        else None

      val inputName = input.prototype.name

      val defaultOverride = defaultsOverride.get(inputName)
      val receivedInput = receivedInputs.get(inputName)
      val receivedSource = sourcesOutputs.get(inputName)
      val receivedImplicit = implicitMap.get(inputName)
      val defaultNonOverride = defaultsNonOverride.get(inputName)

      (defaultOverride, receivedInput, receivedSource, receivedImplicit, defaultNonOverride) match {
        case (Some(parameter), _, _, _, _) ⇒ checkPrototypeMatch(parameter)
        case (None, Some(received), impl, source, param) ⇒
          def providedAfterward = impl.isDefined || source.isDefined || param.isDefined

          checkPrototypeMatch(received.toPrototype) orElse
            (if (received.isOptional && !providedAfterward) Some(OptionalOutput(s, input))
            else None)

        case (None, None, Some(source), impl, param) ⇒
          def providedAfterward = impl.isDefined || param.isDefined

          checkPrototypeMatch(source.prototype) orElse
            (if ((source.mode is Optional) && !providedAfterward)
              Some(OptionalOutput(s, input))
            else None)

        case (None, None, None, Some(impl), _)         ⇒ checkPrototypeMatch(impl)
        case (None, None, None, None, Some(parameter)) ⇒ checkPrototypeMatch(parameter)
        case (None, None, None, None, None) ⇒
          if (!(input.mode is Optional)) Some(MissingInput(s, input)) else None
      }
    }).flatten
  }

  def sourceTypeErrors(mole: Mole, implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks) = {
    val implicitMap = prototypesToMap(implicits)

    val x = (for {
      c ← mole.capsules
      (so: Source) ← sources.getOrElse(c, List.empty)
      (defaultsOverride, defaultsNonOverride) = separateDefaults(so.defaults)
      sl ← mole.slots(c)
      receivedInputs = TreeMap(computeManifests(mole, sources, hooks)(sl).map { p ⇒ p.name -> p }.toSeq: _*)
      i ← so.inputs
    } yield {
      def checkPrototypeMatch(p: Prototype[_]) =
        if (!i.prototype.isAssignableFrom(p)) Some(WrongSourceType(sl, so, i, p))
        else None

      val inputName = i.prototype.name

      val defaultOverride = defaultsOverride.get(inputName)
      val receivedInput = receivedInputs.get(inputName)
      val receivedImplicit = implicitMap.get(inputName)
      val defaultNonOverride = defaultsNonOverride.get(inputName)

      (defaultOverride, receivedInput, receivedImplicit, defaultNonOverride) match {
        case (Some(parameter), _, _, _) ⇒ checkPrototypeMatch(parameter)
        case (None, Some(received), impl, param) ⇒
          def providedAfterward = impl.isDefined || param.isDefined

          checkPrototypeMatch(received.toPrototype) orElse
            (if (received.isOptional && !providedAfterward) Some(OptionalSourceOutput(sl, so, i)) else None)
        case (None, None, Some(impl), _)     ⇒ checkPrototypeMatch(impl)
        case (None, None, None, Some(param)) ⇒ checkPrototypeMatch(param)
        case (None, None, None, None) ⇒
          if (!(i.mode is Optional)) Some(MissingSourceInput(sl, so, i)) else None
      }
    })
    x.flatten
  }

  def typeErrorsTopMole(mole: Mole, implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks) =
    taskTypeErrors(mole)(mole.capsules, implicits, sources, hooks)

  def typeErrorsMoleTask(mole: Mole, implicits: Iterable[Prototype[_]]) =
    taskTypeErrors(mole)(mole.capsules.filterNot(_ == mole.root), implicits, Sources.empty, Hooks.empty)

  def topologyErrors(mole: Mole) = {
    val seen = new HashMap[Capsule, (List[(List[Capsule], Int)])]
    val toProcess = new Queue[(Capsule, Int, List[Capsule])]

    toProcess.enqueue((mole.root, 0, List.empty))
    seen(mole.root) = List((List.empty -> 0))

    while (!toProcess.isEmpty) {
      val (capsule, level, path) = toProcess.dequeue

      Mole.nextCapsules(mole)(capsule, level).foreach {
        case (nCap, nLvl) ⇒
          if (!seen.contains(nCap)) toProcess.enqueue((nCap, nLvl, capsule :: path))
          seen(nCap) = ((capsule :: path) -> nLvl) :: seen.getOrElse(nCap, List.empty)
      }
    }

    seen.filter { case (caps, paths) ⇒ paths.map { case (path, level) ⇒ level }.distinct.size > 1 }.map {
      case (caps, paths) ⇒ new LevelProblem(caps, paths)
    } ++
      seen.flatMap {
        case (caps, paths) ⇒
          paths.filter { case (_, level) ⇒ level < 0 }.map { case (path, level) ⇒ new NegativeLevelProblem(caps, path, level) }
      }
  }

  def duplicatedTransitions(mole: Mole) =
    for {
      end ← mole.capsules
      slot ← mole.slots(end)
      (_, transitions) ← mole.inputTransitions(slot).toList.map { t ⇒ t.start -> t }.groupBy { case (c, _) ⇒ c }
      if (transitions.size > 1)
    } yield DuplicatedTransition(transitions.unzip._2)

  def duplicatedName(mole: Mole, sources: Sources, hooks: Hooks) = {
    def duplicated(data: DataSet) =
      data.data.groupBy(_.prototype.name).filter { case (_, d) ⇒ d.size > 1 }

    mole.capsules.flatMap {
      c ⇒
        duplicated(c.inputs(mole, sources, hooks)).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Input) } ++
          duplicated(c.outputs(mole, sources, hooks)).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Output) }
    }
  }

  private def moleTaskInputMaps(moleTask: MoleTask) =
    (moleTask.mole.root.inputs(moleTask.mole, Sources.empty, Hooks.empty).toList ++
      moleTask.inputs).map(i ⇒ i.prototype.name -> i.prototype).toMap[String, Prototype[_]]

  def moleTaskImplicitsErrors(moleTask: MoleTask, capsule: Capsule) = {
    val inputs = moleTaskInputMaps(moleTask)
    moleTask.implicits.filterNot(inputs.contains).map(i ⇒ MissingMoleTaskImplicit(capsule, i))
  }

  def hookErrors(m: Mole, implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks): Iterable[Problem] = {
    val implicitMap = prototypesToMap(implicits)

    (for {
      c ← m.capsules
      outputs = c.outputs(m, sources, Hooks.empty).toMap
      h ← hooks(c)
      (defaultsOverride, defaultsNonOverride) = separateDefaults(h.defaults)
      i ← h.inputs
    } yield {
      def checkPrototypeMatch(p: Prototype[_]) =
        if (!i.prototype.isAssignableFrom(p)) Some(WrongHookType(c, h, i, p))
        else None

      val inputName = i.prototype.name

      val defaultOverride = defaultsOverride.get(inputName)
      val receivedInput = outputs.get(inputName)
      val receivedImplicit = implicitMap.get(inputName)
      val defaultNonOverride = defaultsNonOverride.get(inputName)

      (defaultOverride, receivedInput, receivedImplicit, defaultNonOverride)

      (defaultOverride, receivedInput, receivedImplicit, defaultNonOverride) match {
        case (Some(parameter), _, _, _) ⇒ checkPrototypeMatch(parameter)
        case (None, Some(received), impl, param) ⇒
          def providedAfterward = impl.isDefined || param.isDefined

          checkPrototypeMatch(received.prototype) orElse
            (if ((received.mode is Optional) && !providedAfterward) Some(OptionalHookOutput(c, h, i)) else None)
        case (None, None, Some(impl), _)     ⇒ checkPrototypeMatch(impl)
        case (None, None, None, Some(param)) ⇒ checkPrototypeMatch(param)
        case (None, None, None, None) ⇒
          if (!(i.mode is Optional)) Some(MissingHookInput(c, h, i)) else None
      }
    }).flatten
  }

  def dataChannelErrors(mole: Mole) =
    mole.dataChannels.filter {
      dc ⇒ mole.level(dc.end) < mole.level(dc.start)
    }.map(DataChannelNegativeLevelProblem(_))

  def apply(mole: Mole, implicits: Context = Context.empty, sources: Sources = Sources.empty, hooks: Hooks = Hooks.empty) = {
    allMoles(mole).flatMap {
      case (m, mt) ⇒
        def moleTaskImplicits(moleTask: MoleTask) = {
          val inputs = moleTaskInputMaps(moleTask)
          moleTask.implicits.flatMap(i ⇒ inputs.get(i))
        }

        (mt match {
          case Some((t, c)) ⇒
            moleTaskImplicitsErrors(t, c) ++
              typeErrorsMoleTask(m, moleTaskImplicits(t)).map { e ⇒ MoleTaskDataFlowProblem(c, e) }
          case None ⇒
            sourceTypeErrors(m, implicits.prototypes, sources, hooks) ++
              hookErrors(m, implicits.prototypes, sources, hooks) ++
              typeErrorsTopMole(m, implicits.prototypes, sources, hooks)
        }) ++
          topologyErrors(m) ++
          duplicatedTransitions(m) ++
          duplicatedName(m, sources, hooks) ++
          dataChannelErrors(m)
    }
  }

}
