/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.data

import org.openmole.core.implementation.validation._
import org.openmole.core.model.mole._
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.workflow.{ CapsuleUI, MoleScene, BuildMoleScene }
import org.openmole.misc.tools.service.Logger
import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.data.Context
import util.{ Failure, Success }
import org.openmole.core.implementation.validation.DataflowProblem.DuplicatedName
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, TaskDataProxyUI }

object CheckData extends Logger {

  def checkMole(scene: MoleScene,
                clear: Boolean = true) = {
    if (clear) StatusBar().clear
    scene match {
      case y: BuildMoleScene ⇒
        y.dataUI.startingCapsule match {
          case Some(x: CapsuleUI) ⇒
            MoleFactory.buildMole(y.dataUI) match {
              case Success((mole, cMap, errs)) ⇒
                val error_capsules = y.dataUI.capsules.values.partition {
                  _.dataUI.task.isDefined
                }
                error_capsules._1.foreach(_.setAsValid)
                error_capsules._2.foreach {
                  _.setAsInvalid("A capsule has to be encapsulated to be run")
                }

                val capsuleMap: Map[ICapsule, CapsuleUI] = cMap.map {
                  case (k, v) ⇒ v -> k
                }

                val sources = capsuleMap.map { c ⇒ c._1 -> c._2.dataUI.sources.map { _.dataUI.coreObject.get } }
                val hooks = capsuleMap.map { c ⇒ c._1 -> c._2.dataUI.hooks.map { _.dataUI.coreObject.get } }

                ToolDataUI.buildUpLevelPrototypes(mole, sources, hooks)
                ToolDataUI.computePrototypeFromAggregation(mole, sources, hooks)
                // Formal validation
                val errors = Validation(mole,
                  Context.empty,
                  sources,
                  hooks)
                errors.isEmpty match {
                  case false ⇒
                    errors.flatMap {
                      _ match {
                        case x: DuplicatedName ⇒
                          /*     val duplicated = x.data.map { _.prototype.name }.toList.distinct
                          List(capsuleMap(x.capsule).dataUI.task).flatten.map { proxy ⇒
                            proxy.dataUI.inputs = proxy.dataUI.inputs.filterNot { p ⇒
                              duplicated.contains(p.dataUI.name)
                            }.toSeq
                            proxy
                          } */
                          displayCapsuleErrors(capsuleMap(x.capsule), x.toString)
                          None
                        case x: DataflowProblem ⇒
                          displayCapsuleErrors(capsuleMap(x.capsule), x)
                          Some(capsuleMap(x.capsule) -> x)
                        case x ⇒
                          logger.info("Error " + x + " not taken into account in the GUI yet.")
                          None
                      }
                    }.groupBy(_._1).map {
                      case (k, v) ⇒ (k, v.map(_._2))
                    }.foreach {
                      case (capsuleUI, e) ⇒
                        capsuleUI.updateErrors(e.toList)
                    }
                  case true ⇒ y.dataUI.capsules.values.foreach {
                    _.updateErrors(List.empty)
                  }
                }
                errs.foreach {
                  case (cui, e) ⇒
                    cui.setAsInvalid(e.getMessage)
                    displayCapsuleErrors(cui, e.getMessage)
                }
                Success(mole, cMap, errs)
              case Failure(l) ⇒ Failure(l)
            }
          case _ ⇒
            Failure(new Throwable("No starting capsule is defined, the Mole can not be built"))
        }
      case _ ⇒ Failure(new Throwable(""))
    }
  }

  def displayCapsuleErrors(capsule: CapsuleUI,
                           e: Problem): Unit = displayCapsuleErrors(capsule, e.getClass.getSimpleName + " : " + e.toString)

  def displayCapsuleErrors(capsule: CapsuleUI,
                           errorMsg: String): Unit = {
    capsule.dataUI.task match {
      case Some(x: TaskDataProxyUI) ⇒ StatusBar().warn(errorMsg, Some(x))
      case None                     ⇒ StatusBar().warn(errorMsg)
    }
  }

  def checkNoEmptyCapsule(scene: MoleScene) =
    scene.dataUI.capsulesInMole.foreach {
      c ⇒
        c.dataUI.task match {
          case Some(x: TaskDataProxyUI) ⇒
          case _                        ⇒ StatusBar().warn("A capsule without task can not be run")
        }
    }

  def fullCheck(scene: MoleScene) = {
    checkMole(scene) match {
      case Success((mole, _, errors)) ⇒
        if (errors.isEmpty) {
          val checkTopo = checkTopology(mole)
          if (checkTopo.isEmpty) Success("")
          else Left(checkTopo)
        }
        else Left(errors.mkString("\n"))
      case Failure(l) ⇒ Failure(l)
    }
  }

  def checkTopology(mole: IMole) =
    Validation.topologyErrors(mole).mkString("\n")
}