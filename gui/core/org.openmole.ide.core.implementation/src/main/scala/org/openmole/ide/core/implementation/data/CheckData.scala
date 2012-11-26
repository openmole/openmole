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

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.validation._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.registry._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Logger
import scala.collection.JavaConversions._

object CheckData extends Logger {

  def checkMole(scene: IMoleScene,
                clear: Boolean = true) = {
    if (clear) StatusBar.clear
    scene match {
      case y: BuildMoleScene ⇒
        y.manager.startingCapsule match {
          case Some(x: ICapsuleUI) ⇒
            MoleMaker.buildMole(y.manager) match {
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
                val prototypeMap: Map[Prototype[_], IPrototypeDataProxyUI] = pMap.map {
                  case (k, v) ⇒ v -> k
                }.toMap

                // Compute implicit input / output
                capsuleMap.foreach {
                  case (caps, capsUI) ⇒
                    capsUI.dataUI.task match {
                      case Some(x: ITaskDataProxyUI) ⇒
                        buildUnknownPrototypes(mole, caps)
                        computeImplicitPrototypes(mole, caps, x)
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
      case Some(x: ITaskDataProxyUI) ⇒ StatusBar.warn(errorMsg, Some(x))
      case None ⇒ StatusBar.warn(errorMsg)
    }
  }

  def buildUnknownPrototypes(mole: IMole, coreCapsule: ICapsule) = {
    (coreCapsule.inputs(mole).toList ++ coreCapsule.outputs(mole)) foreach {
      d ⇒
        if (!MoleMaker.keyPrototypeMapping.keys.contains(KeyPrototypeGenerator(d.prototype))) {
          val (key, dim) = KeyGenerator(d.prototype)
          Proxys.prototypes +=
            new PrototypeDataProxyUI(GenericPrototypeDataUI(d.prototype.name, dim)(d.prototype.`type`), true)
        }
    }
  }

  def computeImplicitPrototypes(
    mole: IMole,
    proxy: ITaskDataProxyUI,
    protoMapping: Map[PrototypeKey, IPrototypeDataProxyUI],
    coreCapsule: ICapsule): Unit = {
    proxy.dataUI.updateImplicits(coreCapsule.inputs(mole).map {
      i ⇒ KeyPrototypeGenerator(i.prototype)
    }.toList
      .filterNot {
        n ⇒
          proxy.dataUI.prototypesIn.map {
            p ⇒ KeyPrototypeGenerator(p)
          }.contains(n)
      }
      .map {
        protoMapping
      },
      coreCapsule.outputs(mole).map {
        i ⇒ KeyPrototypeGenerator(i.prototype)
      }.toList
        .filterNot {
          n ⇒
            proxy.dataUI.prototypesOut.map {
              p ⇒ KeyPrototypeGenerator(p)
            }.contains(n)
        }
        .map {
          protoMapping
        })
  }

  def computeImplicitPrototypes(mole: IMole,
                                capsule: ICapsule,
                                proxy: ITaskDataProxyUI): Unit =
    MoleMaker.taskCoreObject(proxy) match {
      case Right(x: ITask) ⇒
        buildUnknownPrototypes(mole, capsule)
        computeImplicitPrototypes(mole, proxy,
          MoleMaker.keyPrototypeMapping,
          capsule)
      case Left(e: Throwable) ⇒
    }

  def checkNoEmptyCapsule(scene: IMoleScene) =
    scene.manager.capsulesInMole.foreach {
      c ⇒
        c.task match {
          case Some(x: ITaskDataProxyUI) ⇒
          case _ ⇒ StatusBar.warn("A capsule without task can not be run")
        }
    }

  def fullCheck(scene: IMoleScene) = {
    println("++  fullcheck")
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
