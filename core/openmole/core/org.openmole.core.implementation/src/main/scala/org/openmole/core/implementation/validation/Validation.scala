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
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Queue
import org.openmole.core.implementation.data._

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

  def typeErrors(mole: IMole)(capsules: Iterable[ICapsule], implicits: Iterable[Prototype[_]] = List.empty) = {
    def prototypesToMap(prototypes: Iterable[Prototype[_]]) = prototypes.map { i ⇒ i.name -> i }.toMap[String, Prototype[_]]
    val implicitMap = prototypesToMap(implicits)

    capsules.flatMap {
      c ⇒
        mole.slots(c).map {
          s ⇒
            (c, s, TreeMap(computeManifests(mole)(s).map { p ⇒ p.name -> p }.toSeq: _*), {
              def paramsToMap(params: Iterable[Parameter[_]]) =
                params.map {
                  p ⇒ p.variable.prototype.name -> p.variable.prototype
                }.toMap[String, Prototype[_]]

              val (parametersOverride, parameterNonOverride) = c.task.parameters.partition(_.`override`)

              (paramsToMap(parametersOverride), paramsToMap(parameterNonOverride))
            })
        }.flatMap {
          case (capsule, slot, received, (parametersOverride, parameterNonOverride)) ⇒
            capsule.inputs(mole).flatMap {
              input ⇒
                def checkPrototypeMatch(p: Prototype[_]) =
                  if (!input.prototype.isAssignableFrom(p)) Some(WrongType(slot, input, p))
                  else None

                val name = input.prototype.name
                (parametersOverride.get(name), received.get(name), implicitMap.get(name), parameterNonOverride.get(name)) match {
                  case (Some(parameter), _, _, _) ⇒ checkPrototypeMatch(parameter)
                  case (None, Some(received), impl, param) ⇒
                    checkPrototypeMatch(received.toPrototype) orElse
                      (if (received.isOptional && !impl.isDefined && !param.isDefined) Some(OptionalOutput(slot, input)) else None)
                  case (None, None, Some(impl), _) ⇒ checkPrototypeMatch(impl)
                  case (None, None, None, Some(parameter)) ⇒ checkPrototypeMatch(parameter)
                  case (None, None, None, None) ⇒
                    if (!(input.mode is Optional)) Some(MissingInput(slot, input)) else None
                }
            }
        }
    }
  }

  def typeErrorsTopMole(mole: IMole, implicits: Iterable[Prototype[_]]) =
    typeErrors(mole)(mole.capsules, implicits)

  def typeErrorsMoleTask(mole: IMole, implicits: Iterable[Prototype[_]]) =
    typeErrors(mole)(mole.capsules.filterNot(_ == mole.root), implicits)

  def topologyErrors(mole: IMole) = {
    val errors = new ListBuffer[TopologyProblem]
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
    mole.capsules.flatMap {
      end ⇒
        mole.slots(end).flatMap {
          slot ⇒
            mole.inputTransitions(slot).toList.map { t ⇒ t.start -> t }.groupBy { case (c, t) ⇒ c }.filter {
              case (_, transitions) ⇒ transitions.size > 1
            }.map {
              case (_, transitions) ⇒ transitions.map { case (_, t) ⇒ t }
            }
        }
    }.map { t ⇒ new DuplicatedTransition(t) }

  def duplicatedName(mole: IMole) = {
    def duplicated(data: DataSet) =
      data.data.groupBy(_.prototype.name).filter { case (_, d) ⇒ d.size > 1 }

    mole.capsules.flatMap {
      c ⇒
        duplicated(c.inputs(mole)).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Input) } ++
          duplicated(c.outputs(mole)).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Output) }
    }
  }

  private def moleTaskInputMaps(moleTask: IMoleTask) =
    moleTask.mole.root.inputs(moleTask.mole).toList.map(i ⇒ i.prototype.name -> i.prototype).toMap[String, Prototype[_]]

  def moleTaskImplicitsErrors(moleTask: IMoleTask, capsule: ICapsule) = {
    val inputs = moleTaskInputMaps(moleTask)
    moleTask.implicits.filterNot(i ⇒ inputs.contains(i)).map(i ⇒ MissingMoleTaskImplicit(capsule, i))
  }

  def apply(mole: IMole, implicits: Context = Context.empty, sources: Map[ICapsule, Iterable[Source]] = Map.empty, hooks: Map[ICapsule, Iterable[Hook]] = Map.empty) =
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
            typeErrorsTopMole(m, implicits.prototypes)
        }) ++
          topologyErrors(m) ++
          duplicatedTransitions(m) ++
          duplicatedName(m)
    }

}
