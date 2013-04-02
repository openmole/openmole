/*
 * Copyright (C) 31/03/13 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.serializer

import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.core.implementation.workflow._
import java.awt.Point
import org.openmole.ide.core.model.workflow.{ IDataChannelUI, ITransitionUI, IMoleScene }
import org.openmole.ide.misc.tools.util.ID

object MoleData {

  def toScene(moleData: MoleData, proxies: Proxies) = {
    val ui = new MoleUI(moleData.name) {
      override val id = moleData.id
    }
    val scene = new BuildMoleScene(ui)
    ui.plugins = moleData.plugins

    val capsuleUIMap =
      moleData.capsules.map {
        case CapsuleData(c, x, y) ⇒
          val capsuleUI = CapsuleUI.withMenu(scene, c)
          scene.add(capsuleUI, new Point(x, y))
          c -> capsuleUI
      }.toMap

    ui.startingCapsule = moleData.startingCapsule.map(capsuleUIMap(_))

    val slots = moleData.slots.map {
      case SlotData(i, c) ⇒
        val cui = capsuleUIMap(c)
        val slot = cui.addInputSlot
        i -> slot
    }.toMap

    moleData.transitions.foreach {
      t ⇒
        val transition =
          new TransitionUI(
            capsuleUIMap(t.from),
            slots(t.to),
            t.transitionType,
            t.condition,
            t.filtered.map(i ⇒ proxies.prototype(i.id).get))
        scene.add(transition)
    }

    moleData.dataChannels.foreach {
      d ⇒
        val dataChannel =
          new DataChannelUI(
            capsuleUIMap(d.from),
            slots(d.to),
            d.filtered)
        scene.add(dataChannel)
    }
    scene
  }

  def fromScene(scene: IMoleScene) = {
    val slots =
      scene.manager.capsules.flatMap {
        case (_, c) ⇒ c.islots
      }.zipWithIndex.toMap

    val capsules =
      scene.manager.capsules.map {
        case (_, c) ⇒ CapsuleData(c.dataUI, c.x, c.y)
      }

    val transitions =
      scene.manager.connectors.flatMap {
        case (_, transition: ITransitionUI) ⇒
          Some(
            new TransitionData(
              transition.source.dataUI,
              slots(transition.target),
              transition.transitionType,
              transition.condition,
              transition.filteredPrototypes))
        case (_, _) ⇒ None
      }

    val dataChannels =
      scene.manager.connectors.flatMap {
        case (_, dataChannel: IDataChannelUI) ⇒
          Some(
            new DataChannelData(
              dataChannel.source.dataUI,
              slots(dataChannel.target),
              dataChannel.filteredPrototypes))
        case (_, _) ⇒ None
      }

    new MoleData(
      scene.manager.id,
      scene.manager.name,
      scene.manager.startingCapsule.map(_.dataUI),
      capsules,
      slots.map { case (c, i) ⇒ SlotData(i, c.capsule.dataUI) },
      transitions,
      dataChannels,
      scene.manager.plugins)

  }

  case class CapsuleData(capsule: ICapsuleDataUI, x: Int, y: Int)
  case class SlotData(index: Int, capsule: ICapsuleDataUI)

}

import MoleData._

class MoleData(
  val id: ID.Type,
  val name: String,
  val startingCapsule: Option[ICapsuleDataUI],
  val capsules: Iterable[CapsuleData],
  val slots: Iterable[SlotData],
  val transitions: Iterable[TransitionData],
  val dataChannels: Iterable[DataChannelData],
  val plugins: Iterable[String])