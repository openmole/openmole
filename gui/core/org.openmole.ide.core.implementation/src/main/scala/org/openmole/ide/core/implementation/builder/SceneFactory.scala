/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.builder

import java.awt.Point
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.implementation.workflow.{ DataChannelUI, TransitionUI, CapsuleUI }
import org.openmole.core.model.task.ITask
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.registry.KeyPrototypeGenerator

object SceneFactory {

  def as[T: Manifest](x: Any): T =
    if (manifest.runtimeClass.isInstance(x)) x.asInstanceOf[T]
    else throw new UserBadDataError("The Object " + x + " can not be loaded in the GUI")

  def capsuleUI(caps: ICapsuleUI, scene: IMoleScene, locationPoint: Point): ICapsuleUI = {
    scene.initCapsuleAdd(caps)
    scene.manager.registerCapsuleUI(caps)
    scene.graphScene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    CheckData.checkMole(scene)
    caps
  }

  def capsuleUI(scene: IMoleScene,
                locationPoint: Point,
                taskProxy: Option[ITaskDataProxyUI] = None,
                cType: CapsuleType = new BasicCapsuleType): ICapsuleUI =
    capsuleUI(new CapsuleUI(scene, new CapsuleDataUI(task = taskProxy, capsuleType = cType)), scene, locationPoint)

  def transition(scene: IMoleScene,
                 s: ICapsuleUI,
                 t: IInputSlotWidget,
                 transitionType: TransitionType.Value,
                 cond: Option[String] = None,
                 li: List[IPrototypeDataProxyUI] = List.empty) = {
    if (scene.manager.registerConnector(new TransitionUI(s, t, transitionType, cond, li)))
      scene.createConnectEdge(scene.manager.capsuleID(s), scene.manager.capsuleID(t.capsule), t.index)
  }

  def dataChannel(scene: IMoleScene,
                  s: ICapsuleUI,
                  t: IInputSlotWidget,
                  li: List[IPrototypeDataProxyUI] = List.empty) = {
    if (scene.manager.registerConnector(new DataChannelUI(s, t, li)))
      scene.createConnectEdge(scene.manager.capsuleID(s), scene.manager.capsuleID(t.capsule))
  }

  def prototype(p: Prototype[_]) = KeyPrototypeGenerator.prototype(p)
}