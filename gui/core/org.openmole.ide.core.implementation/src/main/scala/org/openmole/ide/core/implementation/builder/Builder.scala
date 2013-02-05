/*
 * Copyright (C) 2013 Mathieu Leclaire
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

import org.openmole.ide.core.implementation.dataproxy.{ TaskDataProxyUI, SamplingCompositionDataProxyUI }
import org.openmole.core.model.task.ITask
import org.openmole.ide.core.implementation.registry.{ DefaultKey, KeyRegistry }
import org.openmole.ide.core.model.workflow.{ IMoleSceneManager, ICapsuleUI }
import org.openmole.core.implementation.puzzle.Puzzle

object Builder {
  def samplingCompositionUI(g: Boolean) = new SamplingCompositionDataProxyUI(generated = g)

  def taskUI(t: ITask) = new TaskDataProxyUI((KeyRegistry.tasks(new DefaultKey(t.getClass)).buildDataUI))

  def puzzle(capsulesUI: List[ICapsuleUI],
             first: ICapsuleUI,
             lasts: Iterable[ICapsuleUI]) = {
    val capsuleMap = capsulesUI.map {
      c ⇒ c -> MoleMaker.buildCapsule(c.dataUI, c.scene.manager.dataUI)
    }.toMap
    val prototypeMap = MoleMaker.prototypeMapping
    val (transitions, dataChannels, islotMap) = MoleMaker.buildConnectors(capsuleMap, prototypeMap)

    new Puzzle(islotMap(first.islots.head),
      lasts.map { capsuleMap },
      transitions,
      dataChannels,
      List.empty,
      Map.empty,
      Map.empty)
  }

  def firsts(capsulesUI: List[ICapsuleUI]) =
    if (capsulesUI.isEmpty) List.empty
    else {
      val connectorTargets = capsulesUI.head.scene.manager.connectors.toList.map {
        _.target.capsule
      }
      capsulesUI.filterNot {
        connectorTargets.contains
      }
    }

  def lasts(capsulesUI: List[ICapsuleUI]) =
    if (capsulesUI.isEmpty) List.empty
    else capsulesUI.filter {
      c ⇒ capsulesUI.head.scene.manager.capsuleConnections(c.dataUI).isEmpty
    }

}
