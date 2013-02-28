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
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.misc.tools.service.Logger
import org.openmole.ide.core.implementation.builder.MoleFactory

object CheckData extends Logger {

  def checkMole(scene: IMoleScene,
                clear: Boolean = true) = {
    if (clear) StatusBar().clear
    scene match {
      case y: BuildMoleScene ⇒
        y.manager.startingCapsule match {
          case Some(x: ICapsuleUI) ⇒
            MoleFactory.buildMole(y.manager) match {
              case Right((mole, cMap, pMap, errs)) ⇒
                val error_capsules = y.manager.capsules.values.partition {
                  _.dataUI.task.isDefined
                }
                error_capsules._1.foreach(_.setAsValid)
                error_capsules._2.foreach {
                  _.setAsInvalid("A capsule has to be encapsulated to be run")
                }

                val capsuleMap: Map[ICapsule, ICapsuleUI] = cMap.map {
                  case (k, v) ⇒ v -> k
                }

                // Compute implicit input / output
                capsuleMap.foreach {
                  case (caps, capsUI) ⇒
                    capsUI.dataUI.task match {
                      case Some(x: ITaskDataProxyUI) ⇒
                        //ToolDataUI.buildUnknownPrototypes(mole, caps)
                        //x.dataUI.computeImplicitPrototypes(mole, caps)
                        ToolDataUI.computePrototypeFromAggregation(mole)
                      case _ ⇒
                    }
                }

                // Formal validation
                val errors = Validation(mole)
                errors.isEmpty match {
                  case false ⇒
                    errors.flatMap {
                      _ match {
                        case x: DataflowProblem ⇒
                          displayCapsuleErrors(capsuleMap(x.capsule), x.toString)
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
                  case true ⇒ y.manager.capsules.values.foreach {
                    _.updateErrors(List.empty)
                  }
                }
                errs.foreach {
                  case (cui, e) ⇒
                    cui.setAsInvalid(e.getMessage)
                    displayCapsuleErrors(cui, e.getMessage)
                }
                Right(mole, cMap, pMap, errs)
              case Left(l) ⇒ Left(l)
            }
          case _ ⇒
            Left(("No starting capsule is defined, the Mole can not be built"))
        }
      case _ ⇒ Left("")
    }
  }

  def displayCapsuleErrors(capsule: ICapsuleUI,
                           errorMsg: String) = {
    capsule.dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒ StatusBar().warn(errorMsg, Some(x))
      case None ⇒ StatusBar().warn(errorMsg)
    }
  }

  def checkNoEmptyCapsule(scene: IMoleScene) =
    scene.manager.capsulesInMole.foreach {
      c ⇒
        c.task match {
          case Some(x: ITaskDataProxyUI) ⇒
          case _ ⇒ StatusBar().warn("A capsule without taskMap can not be run")
        }
    }

  def fullCheck(scene: IMoleScene) = {
    checkMole(scene) match {
      case Right((mole, _, _, errors)) ⇒
        if (errors.isEmpty) {
          val checkTopo = checkTopology(mole)
          if (checkTopo.isEmpty) Right("")
          else Left(checkTopo)
        } else Left(errors.mkString("\n"))
      case Left(l) ⇒ Left(l)
    }
  }

  def checkTopology(mole: IMole) =
    Validation.topologyErrors(mole).mkString("\n")
}