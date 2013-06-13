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

import org.openmole.core.model.data._
import org.openmole.core.model.execution._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import java.io.File
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.transition._
import org.openmole.ide.misc.tools.check.TypeCheck
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.core.model.workflow._
import scala.collection.mutable.HashMap
import concurrent.stm._
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.implementation.execution.ScenesManager
import util.Try
import scala.Some

object MoleFactory {

  def buildMoleExecution(manager: IMoleUI): Try[(PartialMoleExecution, Iterable[(Environment, String)])] = {
    manager.cacheMole match {
      case Some((mole: IMole, capsuleMap: Map[ICapsuleUI, ICapsule], prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]])) ⇒
        buildMoleExecution(mole, manager, capsuleMap, prototypeMap)
      case _ ⇒ throw new UserBadDataError("The Mole execution can not be built")
    }
  }

  case class HookMapping(val c: ICapsule, val h: IHook)
  case class SourceMapping(val c: ICapsule, val s: ISource)

  def buildMoleExecution(mole: IMole,
                         manager: IMoleUI,
                         capsuleMapping: Map[ICapsuleUI, ICapsule],
                         prototypeMapping: Map[IPrototypeDataProxyUI, Prototype[_]]): Try[(PartialMoleExecution, Iterable[(Environment, String)])] =
    Try {
      val envs = capsuleMapping.flatMap { c ⇒
        c._1.dataUI.environment match {
          case Some(env: IEnvironmentDataProxyUI) ⇒ List((c._2, env.dataUI.coreObject, env.dataUI.name))
          case _                                  ⇒ Nil
        }
      }
      val hookMaping = for {
        c ← capsuleMapping
        h ← c._1.dataUI.hooks
      } yield new HookMapping(c._2, h.dataUI.executionCoreObject(prototypeMapping))

      val sourceMaping = for {
        c ← capsuleMapping
        h ← c._1.dataUI.sources
      } yield new SourceMapping(c._2, h.dataUI.coreObject(prototypeMapping))

      (PartialMoleExecution(
        mole,
        sourceMaping.map { h ⇒ (h.c, h.s) },
        hookMaping.map { h ⇒ (h.c, h.h) },
        envs.map { case (c, e, _) ⇒ c -> new FixedEnvironmentSelection(e) }.toMap,
        capsuleMapping.flatMap { c ⇒
          c._1.dataUI.grouping match {
            case Some(gr: IGroupingDataUI) ⇒ List(c._2 -> gr.coreObject)
            case _                         ⇒ Nil
          }
        }), envs.map { case (_, e, n) ⇒ e -> n })
    }

  def buildSource(sourceUI: ISourceDataUI,
                  protoMapping: Map[IPrototypeDataProxyUI, Prototype[_]]): ISource = sourceUI.coreObject(protoMapping)

  def buildSource(sourceUI: ISourceDataUI): ISource = buildSource(sourceUI, prototypeMapping)

  def buildHook(hookUI: IHookDataUI,
                protoMapping: Map[IPrototypeDataProxyUI, Prototype[_]]): IHook = hookUI.coreObject(protoMapping)

  def buildHook(hookUI: IHookDataUI): IHook = buildHook(hookUI, prototypeMapping)

  def buildMole(manager: IMoleUI): Try[(IMole, Map[ICapsuleUI, ICapsule], Map[IPrototypeDataProxyUI, Prototype[_]], Iterable[(ICapsuleUI, Throwable)])] =
    Try {
      if (manager.startingCapsule.isDefined) {
        val prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]] = Proxies.instance.prototypes.map {
          p ⇒ p -> p.dataUI.coreObject
        }.toMap
        val builds = manager.capsules.map {
          c ⇒ (c._2 -> c._2.dataUI.coreObject(manager), None)
        }.toMap

        val capsuleMap: Map[ICapsuleUI, ICapsule] = builds.map {
          case ((cui, c), _) ⇒ cui -> c
        }
        val errors = builds.flatMap {
          case ((_, _), e) ⇒ e
        }
        val (transitions, dataChannels, islotsMap) = buildConnectors(capsuleMap, prototypeMap)
        (new Mole(capsuleMap(manager.startingCapsule.get), transitions, dataChannels), capsuleMap, prototypeMap, errors)
      }
      else throw new UserBadDataError("No starting capsule is defined. The mole construction is not possible. Please define a capsule as a starting capsule.")
    }

  def samplingMapping: Map[ISamplingCompositionDataProxyUI, Sampling] = Proxies.instance.samplings.map {
    s ⇒ s -> s.dataUI.coreObject
  }.toMap

  def prototypeMapping: Map[IPrototypeDataProxyUI, Prototype[_]] = (Proxies.instance.prototypes.toList :::
    List(EmptyDataUIs.emptyPrototypeProxy)).map {
      p ⇒ p -> p.dataUI.coreObject
    }.toMap

  def moleMapping: Map[IMoleScene, IMole] = ScenesManager.moleScenes.map {
    m ⇒ m.graphScene -> buildMole(m.manager).get._1
  }.toMap

  def taskCoreObject(dataUI: ITaskDataUI,
                     plugins: Set[File] = Set.empty): Try[ITask] =
    Try {
      dataUI.coreObject(inputs(dataUI),
        outputs(dataUI),
        parameters(dataUI),
        PluginSet(plugins))
    }

  def inputs(dataUI: ITaskDataUI) = DataSet(dataUI.inputs.map {
    _.dataUI.coreObject
  })

  def outputs(dataUI: ITaskDataUI) = DataSet(dataUI.outputs.map {
    _.dataUI.coreObject
  })

  def parameters(dataUI: ITaskDataUI) =
    ParameterSet(dataUI.inputParameters.flatMap {
      case (protoProxy, v) ⇒
        if (!v.isEmpty) {
          val proto = protoProxy.dataUI.coreObject
          val (msg, obj) = TypeCheck(v, proto)
          obj match {
            case Some(x: Object) ⇒ Some(Parameter(proto.asInstanceOf[Prototype[Any]], x))
            case _               ⇒ None
          }
        }
        else None
    }.toList)

  def inputs(capsuleDataUI: ICapsuleDataUI): DataSet = inputs(capsuleDataUI.task.get.dataUI)

  def outputs(capsuleDataUI: ICapsuleDataUI): DataSet = outputs(capsuleDataUI.task.get.dataUI)

  def parameters(capsuleDataUI: ICapsuleDataUI): ParameterSet = parameters(capsuleDataUI.task.get.dataUI)

  def buildConnectors(capsuleMap: Map[ICapsuleUI, ICapsule],
                      prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]]) = atomic { implicit ctx ⇒
    val islotsMap = new HashMap[IInputSlotWidget, Slot]
    if (capsuleMap.isEmpty) (List.empty, List.empty, islotsMap)
    else {
      val firstCapsule = capsuleMap.head
      val manager = firstCapsule._1.scene.manager
      islotsMap.getOrElseUpdate(firstCapsule._1.islots.head, Slot(capsuleMap(firstCapsule._1)))
      val transitions = capsuleMap.flatMap {
        case (cui, ccore) ⇒
          manager.capsuleConnections.getOrElse(cui.id, TSet.empty).toSet.map { c: IConnectorUI ⇒
            c match {
              case x: ITransitionUI ⇒
                if (capsuleMap.contains(x.target.capsule)) {
                  Some(buildTransition(capsuleMap(x.source),
                    islotsMap.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))),
                    x, prototypeMap))
                }
                else None
              case _ ⇒ None
            }
          }
      }

      val dataChannels = capsuleMap.flatMap {
        case (cui, ccore) ⇒
          manager.capsuleConnections.getOrElse(cui.id, TSet.empty).toSet.map { dc: IConnectorUI ⇒
            dc match {
              case x: IDataChannelUI ⇒
                Some(new DataChannel(
                  capsuleMap(x.source),
                  islotsMap.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))),
                  Block(x.filteredPrototypes.map {
                    p ⇒ prototypeMap(p).name
                  }.toSeq: _*)))
              case _ ⇒ None
            }
          }
      }
      (transitions.flatten, dataChannels.flatten, islotsMap)
    }
  }

  def buildTransition(sourceCapsule: ICapsule,
                      targetSlot: Slot,
                      t: ITransitionUI,
                      prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]]): ITransition = {
    val filtered = t.filteredPrototypes.map {
      p ⇒ prototypeMap(p).name
    }
    val condition: ICondition = if (t.condition.isDefined) Condition(t.condition.get) else ICondition.True
    val a = t.coreObject(sourceCapsule, targetSlot, condition, filtered)
    a
  }
}
