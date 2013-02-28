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
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.registry.PrototypeKey
import org.openmole.ide.core.implementation.registry.KeyPrototypeGenerator
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.data.{ ITaskDataUI, ICapsuleDataUI, IMoleDataUI }
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.core.implementation.task._
import java.io.File
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.tools._
import org.openmole.ide.misc.tools.check.TypeCheck
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.workflow._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.workflow._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.model.sampling.{ ISamplingProxyUI, IDomainProxyUI }
import org.openmole.core.model.domain.{ Finite, Domain }
import org.openmole.ide.core.implementation.execution.ScenesManager
import util.Try

object MoleFactory {

  def buildMoleExecution(mole: IMole,
                         manager: IMoleSceneManager,
                         hooks: List[(ICapsule, IHook)],
                         capsuleMap: Map[ICapsuleUI, ICapsule],
                         groupingStrategies: List[(Grouping, ICapsule)]): Try[(ExecutionContext ⇒ IMoleExecution, Iterable[(Environment, String)])] =
    Try {
      var envs = new HashSet[(Environment, String)]
      val strat = new ListBuffer[(ICapsule, EnvironmentSelection)]

      manager.capsules.values.foreach {
        c ⇒
          c.dataUI.environment match {
            case Some(x: IEnvironmentDataProxyUI) ⇒
              val env = x.dataUI.coreObject
              envs += env -> x.dataUI.name
              strat += capsuleMap(c) -> new FixedEnvironmentSelection(env)
            case _ ⇒
          }
      }
      //TODO implement sources
      (
        MoleExecution.partial(
          mole,
          Iterable.empty,
          hooks,
          strat.toMap,
          groupingStrategies.map {
            case (s, c) ⇒ c -> s
          }.toMap),
          envs.toSet)
    }

  def buildMole(manager: IMoleSceneManager): Either[String, (IMole, Map[ICapsuleUI, ICapsule], Map[IPrototypeDataProxyUI, Prototype[_]], Iterable[(ICapsuleUI, Throwable)])] = {
    try {
      if (manager.startingCapsule.isDefined) {
        val prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]] = Proxys.prototypes.map {
          p ⇒ p -> p.dataUI.coreObject
        }.toMap
        val builds = manager.capsules.map {
          c ⇒
            (c._2 -> buildCapsule(c._2.dataUI, manager.dataUI), None)
        }.toMap

        val capsuleMap: Map[ICapsuleUI, ICapsule] = builds.map {
          case ((cui, c), _) ⇒ cui -> c
        }
        val errors = builds.flatMap {
          case ((_, _), e) ⇒ e
        }

        val (transitions, dataChannels, islotsMap) = buildConnectors(capsuleMap, prototypeMap)

        Right((new Mole(capsuleMap(manager.startingCapsule.get), transitions, dataChannels), capsuleMap, prototypeMap, errors))
      } else throw new UserBadDataError("No starting capsule is defined. The mole construction is not possible. Please define a capsule as a starting capsule.")
    } catch {
      case e: Throwable ⇒
        Left(e.getMessage)
    }
  }

  def samplingMapping: Map[ISamplingCompositionDataProxyUI, Sampling] = Proxys.samplings.map {
    s ⇒ s -> s.dataUI.coreObject
  }.toMap

  def prototypeMapping: Map[IPrototypeDataProxyUI, Prototype[_]] = (Proxys.prototypes.toList :::
    List(EmptyDataUIs.emptyPrototypeProxy)).map {
      p ⇒ p -> p.dataUI.coreObject
    }.toMap

  def moleMapping: Map[IMoleScene, IMole] = ScenesManager.moleScenes.map {
    m ⇒ m.graphScene -> buildMole(m.manager).right.get._1
  }.toMap

  def buildCapsule(
    proxy: ITaskDataProxyUI,
    plugins: Set[File] = Set.empty,
    capsuleType: CapsuleType = new BasicCapsuleType) =
    taskCoreObject(proxy.dataUI, plugins) match {
      case Right(x: ITask) ⇒ capsuleType match {
        case y: MasterCapsuleType ⇒
          new MasterCapsule(x, y.persistList.map {
            _.dataUI.name
          }.toSet)
        case y: StrainerCapsuleType ⇒
          val sc = new StrainerCapsule(x)
          sc
        case _ ⇒ new Capsule(x)
      }
      case Left(x: Throwable) ⇒ new Capsule(EmptyTask(proxy.dataUI.name))
    }

  def buildCapsule(
    capsuleDataUI: ICapsuleDataUI,
    moleDataUI: IMoleDataUI): ICapsule =
    capsuleDataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        buildCapsule(x, moleDataUI.plugins.map {
          p ⇒ new File(p)
        }.toSet, capsuleDataUI.capsuleType)
      case _ ⇒
        StatusBar().inform("A capsule without Task can not be run")
        new Capsule(EmptyTask("None"))

    }

  def taskCoreObject(dataUI: ITaskDataUI,
                     plugins: Set[File] = Set.empty): Either[Throwable, ITask] =
    try {
      Right(dataUI.coreObject(inputs(dataUI),
        outputs(dataUI),
        parameters(dataUI),
        PluginSet(plugins)))
    } catch {
      case e: Throwable ⇒
        Left(e)
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
            case _ ⇒ None
          }
        } else None
    }.toList)

  def inputs(capsuleDataUI: ICapsuleDataUI): DataSet = inputs(capsuleDataUI.task.get.dataUI)

  def outputs(capsuleDataUI: ICapsuleDataUI): DataSet = outputs(capsuleDataUI.task.get.dataUI)

  def parameters(capsuleDataUI: ICapsuleDataUI): ParameterSet = parameters(capsuleDataUI.task.get.dataUI)

  def buildConnectors(capsuleMap: Map[ICapsuleUI, ICapsule],
                      prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]]) = {
    val islotsMap = new HashMap[IInputSlotWidget, Slot]
    if (capsuleMap.isEmpty) (List.empty, List.empty, islotsMap)
    else {
      val firstCapsule = capsuleMap.head
      val manager = firstCapsule._1.scene.manager
      islotsMap.getOrElseUpdate(firstCapsule._1.islots.head, Slot(capsuleMap(firstCapsule._1)))
      val transitions = capsuleMap.flatMap {
        case (cui, ccore) ⇒
          manager.capsuleConnections(cui.dataUI).flatMap {
            _ match {
              case x: ITransitionUI ⇒
                if (capsuleMap.contains(x.target.capsule))
                  Some(buildTransition(capsuleMap(x.source),
                    islotsMap.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))),
                    x, prototypeMap))
                else None
              case _ ⇒ None
            }
          }
      }

      val dataChannels = capsuleMap.flatMap {
        case (cui, ccore) ⇒
          manager.capsuleConnections(cui.dataUI).flatMap {
            _ match {
              case x: IDataChannelUI ⇒
                Some(
                  new DataChannel(
                    capsuleMap(x.source),
                    islotsMap.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))),
                    Block(x.filteredPrototypes.map {
                      p ⇒ prototypeMap(p).name
                    }.toSeq: _*)))
              case _ ⇒ None
            }
          }
      }
      (transitions, dataChannels, islotsMap)
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
    t.coreObject(sourceCapsule, targetSlot, condition, filtered)
  }
}
