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

package org.openmole.core.implementation.validation

import org.openmole.core.implementation.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import TypeUtil.computeManifests
import scala.collection.immutable.TreeMap
import org.openmole.core.model.task._
import org.openmole.misc.tools.obj.ClassUtils._
import DataflowProblem._
import TopologyProblem._
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue

object Validation {

  def allMoles(mole: IMole) =
    (mole, None) ::
      mole.capsules.flatMap {
        c ⇒
          c.task match {
            case mt: IMoleTask ⇒ Some(mt.mole -> Some(mt -> c))
            case _ ⇒ None
          }
      }.toList

  private def paramsToMap(params: Iterable[Parameter[_]]) =
    params.map {
      p ⇒ p.variable.prototype.name -> p.variable.prototype
    }.toMap[String, Prototype[_]]

  private def prototypesToMap(prototypes: Iterable[Prototype[_]]) = prototypes.map { i ⇒ i.name -> i }.toMap[String, Prototype[_]]

  private def separateParameters(p: ParameterSet) = {
    val (po, pno) = p.partition(_.`override`)
    (paramsToMap(po), paramsToMap(pno))
  }

  def taskTypeErrors(mole: IMole)(capsules: Iterable[ICapsule], implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks) = {

    val implicitMap = prototypesToMap(implicits)

    (for {
      c ← capsules
      sourcesOutputs = TreeMap(sources(c).flatMap(_.outputs).map(o ⇒ o.prototype.name -> o).toSeq: _*)
      s ← mole.slots(c)
      receivedInputs = TreeMap(computeManifests(mole, sources, hooks)(s).map { p ⇒ p.name -> p }.toSeq: _*)
      (parametersOverride, parametersNonOverride) = separateParameters(c.task.parameters)
      input ← c.task.inputs
    } yield {
      def checkPrototypeMatch(p: Prototype[_]) =
        if (!input.prototype.isAssignableFrom(p)) Some(WrongType(s, input, p))
        else None

      val inputName = input.prototype.name

      val parameterOverride = parametersOverride.get(inputName)
      val receivedInput = receivedInputs.get(inputName)
      val receivedSource = sourcesOutputs.get(inputName)
      val receivedImplicit = implicitMap.get(inputName)
      val parameterNonOverride = parametersNonOverride.get(inputName)

      (parameterOverride, receivedInput, receivedSource, receivedImplicit, parameterNonOverride) match {
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

        case (None, None, None, Some(impl), _) ⇒ checkPrototypeMatch(impl)
        case (None, None, None, None, Some(parameter)) ⇒ checkPrototypeMatch(parameter)
        case (None, None, None, None, None) ⇒
          if (!(input.mode is Optional)) Some(MissingInput(s, input)) else None
      }
    }).flatten
  }

  def sourceTypeErrors(mole: IMole, implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks) = {
    val implicitMap = prototypesToMap(implicits)

    (for {
      c ← mole.capsules
      so ← sources.getOrElse(c, List.empty)
      (parametersOverride, parametersNonOverride) = separateParameters(so.parameters)
      sl ← mole.slots(c)
      receivedInputs = TreeMap(computeManifests(mole, sources, hooks)(sl).map { p ⇒ p.name -> p }.toSeq: _*)
      i ← so.inputs
    } yield {
      def checkPrototypeMatch(p: Prototype[_]) =
        if (!i.prototype.isAssignableFrom(p)) Some(WrongSourceType(sl, so, i, p))
        else None

      val inputName = i.prototype.name

      val parameterOverride = parametersOverride.get(inputName)
      val receivedInput = receivedInputs.get(inputName)
      val receivedImplicit = implicitMap.get(inputName)
      val parameterNonOverride = parametersNonOverride.get(inputName)

      (parameterOverride, receivedInput, receivedImplicit, parameterNonOverride) match {
        case (Some(parameter), _, _, _) ⇒ checkPrototypeMatch(parameter)
        case (None, Some(received), impl, param) ⇒
          def providedAfterward = impl.isDefined || param.isDefined

          checkPrototypeMatch(received.toPrototype) orElse
            (if (received.isOptional && !providedAfterward) Some(OptionalSourceOutput(sl, so, i)) else None)
        case (None, None, Some(impl), _) ⇒ checkPrototypeMatch(impl)
        case (None, None, None, Some(param)) ⇒ checkPrototypeMatch(param)
        case (None, None, None, None) ⇒
          if (!(i.mode is Optional)) Some(MissingSourceInput(sl, so, i)) else None
      }
    }).flatten
  }

  def typeErrorsTopMole(mole: IMole, implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks) =
    taskTypeErrors(mole)(mole.capsules, implicits, sources, hooks)

  def typeErrorsMoleTask(mole: IMole, implicits: Iterable[Prototype[_]]) =
    taskTypeErrors(mole)(mole.capsules.filterNot(_ == mole.root), implicits, Sources.empty, Hooks.empty)

  def topologyErrors(mole: IMole) = {
    val seen = new HashMap[ICapsule, (List[(List[ICapsule], Int)])]
    val toProcess = new Queue[(ICapsule, Int, List[ICapsule])]

    toProcess.enqueue((mole.root, 0, List.empty))
    seen(mole.root) = List((List.empty -> 0))

    while (!toProcess.isEmpty) {
      val (capsule, level, path) = toProcess.dequeue

      Mole.nextCaspules(mole)(capsule, level).foreach {
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

  def duplicatedTransitions(mole: IMole) =
    for {
      end ← mole.capsules
      slot ← mole.slots(end)
      (_, transitions) ← mole.inputTransitions(slot).toList.map { t ⇒ t.start -> t }.groupBy { case (c, _) ⇒ c }
      if (transitions.size > 1)
    } yield {
      new DuplicatedTransition(transitions.unzip._2)
    }

  def duplicatedName(mole: IMole, sources: Sources, hooks: Hooks) = {
    def duplicated(data: DataSet) =
      data.data.groupBy(_.prototype.name).filter { case (_, d) ⇒ d.size > 1 }

    mole.capsules.flatMap {
      c ⇒
        duplicated(c.inputs(mole, sources, hooks)).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Input) } ++
          duplicated(c.outputs(mole, sources, hooks)).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Output) }
    }
  }

  private def moleTaskInputMaps(moleTask: IMoleTask) =
    moleTask.mole.root.inputs(moleTask.mole, Sources.empty, Hooks.empty).toList.map(i ⇒ i.prototype.name -> i.prototype).toMap[String, Prototype[_]]

  def moleTaskImplicitsErrors(moleTask: IMoleTask, capsule: ICapsule) = {
    val inputs = moleTaskInputMaps(moleTask)
    moleTask.implicits.filterNot(i ⇒ inputs.contains(i)).map(i ⇒ MissingMoleTaskImplicit(capsule, i))
  }

  def hookErrors(m: IMole, implicits: Iterable[Prototype[_]], sources: Sources, hooks: Hooks): Iterable[Problem] = {
    val implicitMap = prototypesToMap(implicits)

    (for {
      c ← m.capsules
      outputs = c.outputs(m, sources, Hooks.empty).toMap
      h ← hooks(c)
      (parametersOverride, parametersNonOverride) = separateParameters(h.parameters)
      i ← h.inputs
    } yield {
      def checkPrototypeMatch(p: Prototype[_]) =
        if (!i.prototype.isAssignableFrom(p)) Some(WrongHookType(c, h, i, p))
        else None

      val inputName = i.prototype.name

      val parameterOverride = parametersOverride.get(inputName)
      val receivedInput = outputs.get(inputName)
      val receivedImplicit = implicitMap.get(inputName)
      val parameterNonOverride = parametersNonOverride.get(inputName)

      (parameterOverride, receivedInput, receivedImplicit, parameterNonOverride)

      (parameterOverride, receivedInput, receivedImplicit, parameterNonOverride) match {
        case (Some(parameter), _, _, _) ⇒ checkPrototypeMatch(parameter)
        case (None, Some(received), impl, param) ⇒
          def providedAfterward = impl.isDefined || param.isDefined

          checkPrototypeMatch(received.prototype) orElse
            (if ((received.mode is Optional) && !providedAfterward) Some(OptionalHookOutput(c, h, i)) else None)
        case (None, None, Some(impl), _) ⇒ checkPrototypeMatch(impl)
        case (None, None, None, Some(param)) ⇒ checkPrototypeMatch(param)
        case (None, None, None, None) ⇒
          if (!(i.mode is Optional)) Some(MissingHookInput(c, h, i)) else None
      }
    }).flatten
  }

  def apply(mole: IMole, implicits: Context = Context.empty, sources: Sources = Sources.empty, hooks: Hooks = Hooks.empty) =
    allMoles(mole).flatMap {
      case (m, mt) ⇒
        def moleTaskImplicits(moleTask: IMoleTask) = {
          val inputs = moleTaskInputMaps(moleTask)
          moleTask.implicits.flatMap(i ⇒ inputs.get(i))
        }

        (mt match {
          case Some((t, c)) ⇒
            moleTaskImplicitsErrors(t, c) ++
              typeErrorsMoleTask(m, moleTaskImplicits(t))
          case None ⇒
            sourceTypeErrors(m, implicits.prototypes, sources, hooks) ++
              hookErrors(m, implicits.prototypes, sources, hooks) ++
              typeErrorsTopMole(m, implicits.prototypes, sources, hooks)
        }) ++
          topologyErrors(m) ++
          duplicatedTransitions(m) ++
          duplicatedName(m, sources, hooks)
    }

}
