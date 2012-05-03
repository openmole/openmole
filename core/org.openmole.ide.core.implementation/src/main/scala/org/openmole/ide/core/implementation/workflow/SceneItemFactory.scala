/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.workflow

import java.awt.Point
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.workflow.IMoleScene

object SceneItemFactory {

  def createCapsule(caps: ICapsuleUI, scene: IMoleScene, locationPoint: Point): ICapsuleUI = {
    scene.initCapsuleAdd(caps)
    scene.manager.registerCapsuleUI(caps)
    scene.graphScene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    CheckData.checkMole(scene.manager)
    caps
  }

  def createCapsule(scene: IMoleScene, locationPoint: Point): ICapsuleUI = createCapsule(new CapsuleUI(scene), scene, locationPoint)

  def createTransition(scene: IMoleScene, s: ICapsuleUI, t: IInputSlotWidget, transitionType: TransitionType.Value, cond: Option[String]) = {
    if (scene.manager.registerTransition(s, t, transitionType, cond))
      scene.createConnectEdge(scene.manager.capsuleID(s), scene.manager.capsuleID(t.capsule))
  }

  def createDataChannel(scene: IMoleScene, s: ICapsuleUI, t: ICapsuleUI, li: List[IPrototypeDataProxyUI]) = {
    if (scene.manager.registerDataChannel(s, t, li))
      scene.createDataChannelEdge(scene.manager.capsuleID(s), scene.manager.capsuleID(t))
  }
}