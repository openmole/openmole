/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.builder

import org.openmole.core.model.execution._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.transition._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.mole._
import org.openmole.core.model.transition._
import org.openmole.ide.core.implementation.data.{ GroupingDataUI }
import org.openmole.ide.core.implementation.dataproxy.{ SamplingCompositionDataProxyUI, EnvironmentDataProxyUI, Proxies }
import scala.collection.mutable.HashMap
import concurrent.stm._
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.implementation.execution.ScenesManager
import util.Try
import org.openmole.ide.core.implementation.workflow._
import scala.Some

object MoleFactory {

  def buildMoleExecution(manager: IMoleUI): Try[(PartialMoleExecution, Iterable[(Environment, String)])] = {
    manager.cacheMole match {
      case Some((mole: IMole, capsuleMap: Map[CapsuleUI, ICapsule])) ⇒
        buildMoleExecution(mole, manager, capsuleMap)
      case _ ⇒ throw new UserBadDataError("The Mole execution can not be built")
    }
  }

  case class HookMapping(c: ICapsule, h: IHook)
  case class SourceMapping(c: ICapsule, s: ISource)

  def buildMoleExecution(mole: IMole,
                         manager: IMoleUI,
                         capsuleMapping: Map[CapsuleUI, ICapsule]): Try[(PartialMoleExecution, Iterable[(Environment, String)])] =
    Try {
      val (envs, envNames) = capsuleMapping.flatMap {
        case (cProxy, c) ⇒
          cProxy.dataUI.environment match {
            case Some(env: EnvironmentDataProxyUI) ⇒
              val unauthenticatedEnv = env.dataUI.coreObject.get
              Some((c -> unauthenticatedEnv, unauthenticatedEnv -> env.dataUI.name))
            case _ ⇒ Nil
          }
      }.unzip

      val hookMaping = for {
        c ← capsuleMapping
        h ← c._1.dataUI.hooks
      } yield new HookMapping(c._2, h.dataUI.executionCoreObject.get)

      val sourceMaping = for {
        c ← capsuleMapping
        h ← c._1.dataUI.sources
      } yield new SourceMapping(c._2, h.dataUI.executionCoreObject.get)

      (PartialMoleExecution(
        mole,
        sourceMaping.map { h ⇒ (h.c, h.s) },
        hookMaping.map { h ⇒ (h.c, h.h) },
        envs.toMap,
        capsuleMapping.flatMap { c ⇒
          c._1.dataUI.grouping match {
            case Some(gr: GroupingDataUI) ⇒ List(c._2 -> gr.coreObject.get)
            case _                        ⇒ Nil
          }
        }), envNames)
    }

  def buildMole(manager: IMoleUI): Try[(IMole, Map[CapsuleUI, ICapsule], Iterable[(CapsuleUI, Throwable)])] =
    Try {
      if (manager.startingCapsule.isDefined) {
        val builds = manager.capsules.map {
          c ⇒
            val (caps, error) = c._2.dataUI.coreObject(manager)
            (c._2 -> caps, error)
        }.toMap

        val capsuleMap: Map[CapsuleUI, ICapsule] = builds.map {
          case ((cui, c), _) ⇒ cui -> c
        }
        val errors = builds.flatMap {
          case ((cui, _), e) ⇒ e.map(cui -> _)
        }
        val (transitions, dataChannels, islotsMap) = buildConnectors(capsuleMap)
        (new Mole(capsuleMap(manager.startingCapsule.get), transitions, dataChannels), capsuleMap, errors)
      }
      else throw new UserBadDataError("No starting capsule is defined. The mole construction is not possible. Please define a capsule as a starting capsule.")
    }

  def samplingMapping: Map[SamplingCompositionDataProxyUI, Sampling] = Proxies.instance.samplings.map {
    s ⇒ s -> s.dataUI.coreObject.get
  }.toMap

  def moleMapping: Map[MoleScene, IMole] = ScenesManager.moleScenes.map {
    m ⇒ m.graphScene -> buildMole(m.dataUI).get._1
  }.toMap

  def buildConnectors(capsuleMap: Map[CapsuleUI, ICapsule]) = atomic { implicit ctx ⇒
    val islotsMap = new HashMap[InputSlotWidget, Slot]
    if (capsuleMap.isEmpty) (List.empty, List.empty, islotsMap)
    else {
      val firstCapsule = capsuleMap.head
      val manager = firstCapsule._1.scene.dataUI
      islotsMap.getOrElseUpdate(firstCapsule._1.inputSlots.head, Slot(capsuleMap(firstCapsule._1)))
      val transitions = capsuleMap.flatMap {
        case (cui, ccore) ⇒
          manager.capsuleConnections.getOrElse(cui.id, TSet.empty).toSet.map { c: ConnectorUI ⇒
            c match {
              case x: TransitionUI ⇒
                if (capsuleMap.contains(x.target.capsule)) {
                  Some(buildTransition(capsuleMap(x.source),
                    islotsMap.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))), x))
                }
                else None
              case _ ⇒ None
            }
          }
      }

      val dataChannels = capsuleMap.flatMap {
        case (cui, ccore) ⇒
          manager.capsuleConnections.getOrElse(cui.id, TSet.empty).toSet.map { dc: ConnectorUI ⇒
            dc match {
              case x: DataChannelUI ⇒
                Some(new DataChannel(
                  capsuleMap(x.source),
                  islotsMap.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))),
                  Block(x.filteredPrototypes.map { _.dataUI.coreObject.get.name }.toSeq: _*)))
              case _ ⇒ None
            }
          }
      }
      (transitions.flatten, dataChannels.flatten, islotsMap)
    }
  }

  def buildTransition(sourceCapsule: ICapsule,
                      targetSlot: Slot,
                      t: TransitionUI): Transition = {
    val filtered = t.filteredPrototypes.map { _.dataUI.coreObject.get.name }
    val condition: ICondition = if (t.condition.isDefined) Condition(t.condition.get) else ICondition.True
    val a = t.coreObject(sourceCapsule, targetSlot, condition, filtered)
    a
  }
}
