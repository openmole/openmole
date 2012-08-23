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

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameter
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole
import TypeUtil.receivedTypes
import org.openmole.core.model.data.DataModeMask._
import scala.collection.immutable.TreeMap
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IMoleTask
import org.openmole.misc.tools.obj.ClassUtils._
import DataflowProblem._
import TopologyProblem._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Queue
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.tools.LevelComputing._

object Validation {

  def allMoles(mole: IMole) =
    (mole, false) ::
      mole.capsules.flatMap {
        _.task match {
          case Some(task) ⇒
            task match {
              case mt: IMoleTask ⇒ Some((mt.mole, true))
              case _ ⇒ None
            }
          case _ ⇒ None
        }
      }.toList

  def typeErrors(capsules: Iterable[ICapsule], implicits: Iterable[IPrototype[_]] = List.empty) = {
    val implicitMap = implicits.map { i ⇒ i.name -> i }.toMap[String, IPrototype[_]]
    capsules.flatMap {
      c ⇒
        c.intputSlots.map {
          s ⇒
            (c, s, TreeMap(receivedTypes(s).map { p ⇒ p.name -> p }.toSeq: _*), c.task match {
              case Some(t) ⇒
                def paramsToMap(params: Iterable[IParameter[_]]) =
                  params.map {
                    p ⇒ p.variable.prototype.name -> p.variable.prototype
                  }.toMap[String, IPrototype[_]]

                val (parametersOverride, parameterNonOverride) = t.parameters.partition(_.`override`)

                (paramsToMap(parametersOverride), paramsToMap(parameterNonOverride))
              case None ⇒ (Map.empty[String, IPrototype[_]], Map.empty[String, IPrototype[_]])
            })
        }.flatMap {

          case (capsule, slot, received, (parametersOverride, parameterNonOverride)) ⇒
            capsule.inputs.filterNot(_.mode is optional).flatMap {
              input ⇒
                def checkPrototypeMatch(p: IPrototype[_]) =
                  if (!input.prototype.isAssignableFrom(p)) Some(new WrongType(slot, input, p))
                  else None

                val name = input.prototype.name
                (parametersOverride.get(name), received.get(name), implicitMap.get(name), parameterNonOverride.get(name)) match {
                  case (Some(parameter), _, _, _) ⇒ checkPrototypeMatch(parameter)
                  case (None, Some(recieved), _, _) ⇒ checkPrototypeMatch(recieved)
                  case (None, None, Some(impl), _) ⇒ checkPrototypeMatch(impl)
                  case (None, None, None, Some(parameter)) ⇒ checkPrototypeMatch(parameter)
                  case (None, None, None, None) ⇒ Some(new MissingInput(slot, input))
                }
            }
        }
    }
  }

  def typeErrorsTopMole(mole: IMole, implicits: Iterable[IPrototype[_]]) = typeErrors(mole.capsules, implicits)
  def typeErrorsMoleTask(mole: IMole, implicits: Iterable[IPrototype[_]]) = typeErrors(mole.capsules.drop(1), implicits)

  def topologyErrors(mole: IMole) = {
    val errors = new ListBuffer[TopologyProblem]
    val seen = new HashMap[ICapsule, (List[(List[ICapsule], Int)])]
    val toProcess = new Queue[(ICapsule, Int, List[ICapsule])]

    toProcess.enqueue((mole.root, 0, List.empty))
    seen(mole.root) = List((List.empty -> 0))

    while (!toProcess.isEmpty) {
      val (capsule, level, path) = toProcess.dequeue

      nextCaspules(capsule, level).foreach {
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
        end.intputSlots.flatMap {
          slot ⇒
            slot.transitions.toList.map { t ⇒ t.start -> t }.groupBy { case (c, t) ⇒ c }.filter {
              case (_, transitions) ⇒ transitions.size > 1
            }.map {
              case (_, transitions) ⇒ transitions.map { case (_, t) ⇒ t }
            }
        }
    }.map { t ⇒ new DuplicatedTransition(t) }

  def duplicatedName(mole: IMole) = {
    def duplicated(data: IDataSet) =
      data.data.groupBy(_.prototype.name).filter { case (_, d) ⇒ d.size > 1 }

    mole.capsules.flatMap {
      c ⇒
        duplicated(c.inputs).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Input) } ++
          duplicated(c.outputs).map { case (name, data) ⇒ new DuplicatedName(c, name, data, Output) }
    }
  }

  def apply(mole: IMole) =
    allMoles(mole).flatMap {
      case (m, mt) ⇒
        if (mt) typeErrorsMoleTask(m, m.implicits.values.map { _.prototype }) else typeErrorsTopMole(m, m.implicits.values.map { _.prototype }) ++
          topologyErrors(m) ++
          duplicatedTransitions(m) ++
          duplicatedName(m)
    }

}
