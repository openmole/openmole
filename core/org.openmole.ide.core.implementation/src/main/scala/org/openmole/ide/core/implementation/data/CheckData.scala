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
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.validation.DataflowProblem
import org.openmole.core.implementation.validation.Validation
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.registry._
import org.openmole.core.model.mole.IMole
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyFactory
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Logger
import scala.collection.JavaConversions._

object CheckData extends Logger {
  def checkMole(manager: IMoleSceneManager) =
    manager.startingCapsule match {
      case Some(x: ICapsuleUI) ⇒
        val (mole, cMap, pMap, errs) = MoleMaker.buildMole(manager)
        val error_capsules = manager.capsules.values.partition { _.dataUI.task.isDefined }
        error_capsules._1.foreach(_.setAsValid)
        error_capsules._2.foreach { _.setAsInvalid("A capsule has to be encapsulated to be run") }

        val capsuleMap: Map[ICapsule, ICapsuleUI] = cMap.map { case (k, v) ⇒ v -> k }
        val prototypeMap: Map[IPrototype[_], IPrototypeDataProxyUI] = pMap.map { case (k, v) ⇒ v -> k }.toMap

        // Compute implicit input / output
        capsuleMap.foreach {
          case (caps, capsUI) ⇒
            capsUI.dataUI.task match {
              case Some(x: ITaskDataProxyUI) ⇒
                buildUnknownPrototypes(caps)
                computeImplicitPrototypes(x)
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
                  Some(capsuleMap(x.capsule) -> (prototypeMap(x.data.prototype), x))
                case x ⇒
                  logger.info("Error " + x + " not taken into account in the GUI yet.")
                  None
              }
            }.groupBy(_._1).map { case (k, v) ⇒ (k, v.map(_._2)) }.foreach {
              case (capsuleUI, e) ⇒
                capsuleUI.updateErrors(e.toList)
            }
          case true ⇒ manager.capsules.values.foreach { _.updateErrors(List.empty) }
        }
        Some(mole, capsuleMap, prototypeMap, errs)
        errs.foreach { case (cui, e) ⇒ cui.setAsInvalid(e.getMessage) }
        Some(mole, cMap, pMap, errs)
      case _ ⇒ None
    }

  def buildUnknownPrototypes(coreCapsule: ICapsule) = {
    var protoMapping = MoleMaker.keyPrototypeMapping

    (coreCapsule.inputs.toList ++ coreCapsule.outputs) foreach { d ⇒
      if (!protoMapping.keys.contains(KeyPrototypeGenerator(d.prototype))) {
        val (key, dim) = KeyGenerator(d.prototype: IPrototype[_])
        Proxys.prototypes +=
          new PrototypeDataProxyFactory(KeyRegistry.prototypes(key)).buildDataProxyUI(d.prototype, true, dim)

      }
    }
  }

  def computeImplicitPrototypes(proxy: ITaskDataProxyUI,
                                protoMapping: Map[PrototypeKey, IPrototypeDataProxyUI],
                                coreCapsule: ICapsule): Unit = {

    proxy.dataUI.implicitPrototypesIn = coreCapsule.inputs.map { i ⇒ KeyPrototypeGenerator(i.prototype) }.toList
      .filterNot { n ⇒ proxy.dataUI.prototypesIn.map { p ⇒ KeyPrototypeGenerator(p) }.contains(n) }
      .map { protoMapping }

    proxy.dataUI.implicitPrototypesOut = coreCapsule.outputs.map { i ⇒ KeyPrototypeGenerator(i.prototype) }.toList
      .filterNot { n ⇒ proxy.dataUI.prototypesOut.map { p ⇒ KeyPrototypeGenerator(p) }.contains(n) }
      .map { protoMapping }
  }

  def computeImplicitPrototypes(proxy: ITaskDataProxyUI): Unit = {
    val coreCapsule = new Capsule(MoleMaker.taskCoreObject(proxy))
    buildUnknownPrototypes(coreCapsule)
    computeImplicitPrototypes(proxy,
      MoleMaker.keyPrototypeMapping,
      coreCapsule)
  }

  def checkTaskProxyImplicitsPrototypes(scene: IMoleScene,
                                        proxy: ITaskDataProxyUI) = {

    try {
      scene.manager.capsules.values.flatMap { _.dataUI.task }.contains(proxy) match {
        case true ⇒ checkMole(scene.manager)
        case false ⇒ computeImplicitPrototypes(proxy)
      }
    } catch { case e: UserBadDataError ⇒ }
  }

  def fullCheck(manager: IMoleSceneManager) = {
    val a = checkMole(manager)
    if (a.isDefined)
      if (a.get._4.isEmpty)
        checkTopology(a.get._1)
  }

  def checkTopology(mole: IMole) = {
    val st = Validation.topologyErrors(mole).mkString("\n")
    if (!st.isEmpty) StatusBar.block(st)
  }
}