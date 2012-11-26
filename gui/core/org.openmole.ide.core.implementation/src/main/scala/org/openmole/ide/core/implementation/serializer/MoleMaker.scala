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

package org.openmole.ide.core.implementation.serializer

import org.openmole.core.model.data._
import org.openmole.core.model.execution._
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.registry.PrototypeKey
import org.openmole.ide.core.implementation.registry.KeyPrototypeGenerator
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.data.IMoleDataUI
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

object MoleMaker {

  def buildMoleExecution(mole: IMole,
                         manager: IMoleSceneManager,
                         hooks: List[(ICapsule, Hook)],
                         capsuleMap: Map[ICapsuleUI, ICapsule],
                         groupingStrategies: List[(Grouping, ICapsule)]): Either[Throwable, (IMoleExecution, Iterable[(Environment, String)])] =
    try {
      var envs = new HashSet[(Environment, String)]
      val strat = new ListBuffer[(ICapsule, EnvironmentSelection)]

      manager.capsules.values.foreach { c ⇒
        c.dataUI.environment match {
          case Some(x: IEnvironmentDataProxyUI) ⇒
            val env = x.dataUI.coreObject
            envs += env -> x.dataUI.name
            strat += capsuleMap(c) -> new FixedEnvironmentSelection(env)
          case _ ⇒
        }
      }
      Right((new MoleExecution(mole, hooks, strat.toMap, groupingStrategies.map { case (s, c) ⇒ c -> s }.toMap), envs.toSet))
    } catch {
      case e: Throwable ⇒
        Left(e)
    }

  def buildMole(manager: IMoleSceneManager): Either[String, (IMole, Map[ICapsuleUI, ICapsule], Map[IPrototypeDataProxyUI, Prototype[_]], Iterable[(ICapsuleUI, Throwable)])] = {
    try {
      if (manager.startingCapsule.isDefined) {
        val prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]] = Proxys.prototypes.map { p ⇒ p -> p.dataUI.coreObject }.toMap
        val builds = manager.capsules.map { c ⇒
          (c._2 -> buildCapsule(c._2.dataUI, manager.dataUI), None)
        }.toMap

        val capsuleMap: Map[ICapsuleUI, ICapsule] = builds.map { case ((cui, c), _) ⇒ cui -> c }
        val errors = builds.flatMap { case ((_, _), e) ⇒ e }

        val islotsMapping = new HashMap[IInputSlotWidget, Slot]

        val transitions = capsuleMap.flatMap {
          case (cui, ccore) ⇒
            manager.capsuleConnections(cui.dataUI).flatMap {
              _ match {
                case x: ITransitionUI ⇒
                  Some(buildTransition(capsuleMap(x.source),
                    islotsMapping.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))),
                    x, prototypeMap))
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
                      islotsMapping.getOrElseUpdate(x.target, Slot(capsuleMap(x.target.capsule))),
                      Filter(x.filteredPrototypes.map { p ⇒ prototypeMap(p).name }.toSeq: _*)))
                case _ ⇒ None
              }
            }
        }

        Right((new Mole(capsuleMap(manager.startingCapsule.get), transitions, dataChannels), capsuleMap, prototypeMap, errors))
      } else throw new UserBadDataError("No starting capsule is defined. The mole construction is not possible. Please define a capsule as a starting capsule.")
    } catch {
      case e: Throwable ⇒
        Left(e.getMessage)
    }
  }

  def prototypeMapping: Map[IPrototypeDataProxyUI, Prototype[_]] = (Proxys.prototypes.toList :::
    List(EmptyDataUIs.emptyPrototypeProxy)).map { p ⇒ p -> p.dataUI.coreObject }.toMap

  def keyPrototypeMapping: Map[PrototypeKey, IPrototypeDataProxyUI] = (Proxys.prototypes.toList :::
    List(EmptyDataUIs.emptyPrototypeProxy)).map { p ⇒ KeyPrototypeGenerator(p) -> p }.toMap

  def buildCapsule(
    proxy: ITaskDataProxyUI,
    plugins: Set[File] = Set.empty,
    capsuleType: CapsuleType = new BasicCapsuleType) =
    taskCoreObject(proxy, plugins) match {
      case Right(x: ITask) ⇒ capsuleType match {
        case y: MasterCapsuleType ⇒
          new MasterCapsule(x, y.persistList.map { _.dataUI.name }.toSet)
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
        buildCapsule(x, moleDataUI.plugins.map { p ⇒ new File(p) }.toSet, capsuleDataUI.capsuleType)
      case _ ⇒
        StatusBar.inform("A capsule without Task can not be run")
        new Capsule(EmptyTask("None"))

    }

  def taskCoreObject(proxy: ITaskDataProxyUI,
                     plugins: Set[File] = Set.empty): Either[Throwable, ITask] =
    try {
      Right(proxy.dataUI.coreObject(inputs(proxy),
        outputs(proxy),
        parameters(proxy),
        PluginSet(plugins)))
    } catch {
      case e: Throwable ⇒
        StatusBar.warn(e.getMessage, Some(proxy), e.getStackTraceString, e.getClass.getCanonicalName)
        Left(e)
    }

  def taskCoreObject(capsuleDataUI: ICapsuleDataUI,
                     plugins: Set[File]): Either[Throwable, ITask] =
    taskCoreObject(capsuleDataUI.task.get, plugins)

  def inputs(proxy: ITaskDataProxyUI) = DataSet(proxy.dataUI.prototypesIn.map { _.dataUI.coreObject })
  def outputs(proxy: ITaskDataProxyUI) = DataSet(proxy.dataUI.prototypesOut.map { _.dataUI.coreObject })

  def parameters(proxy: ITaskDataProxyUI) =
    ParameterSet(proxy.dataUI.inputParameters.flatMap {
      case (protoProxy, v) ⇒
        if (!v.isEmpty) {
          val proto = protoProxy.dataUI.coreObject
          val (msg, obj) = TypeCheck(v, proto)
          obj match {
            case Some(x: Object) ⇒ Some(new Parameter(proto.asInstanceOf[Prototype[Any]], x))
            case _ ⇒ None
          }
        } else None
    }.toList)

  def inputs(capsuleDataUI: ICapsuleDataUI): DataSet = inputs(capsuleDataUI.task.get)

  def outputs(capsuleDataUI: ICapsuleDataUI): DataSet = outputs(capsuleDataUI.task.get)

  def parameters(capsuleDataUI: ICapsuleDataUI): ParameterSet = parameters(capsuleDataUI.task.get)

  def buildTransition(
    sourceCapsule: ICapsule,
    targetSlot: Slot,
    t: ITransitionUI,
    prototypeMap: Map[IPrototypeDataProxyUI, Prototype[_]]): ITransition = {
    val filtered = t.filteredPrototypes.map { p ⇒ prototypeMap(p).name }
    val condition: ICondition = if (t.condition.isDefined) new Condition(t.condition.get) else ICondition.True
    t.transitionType match {
      case BASIC_TRANSITION ⇒ new Transition(sourceCapsule, targetSlot, condition, Filter(filtered: _*))
      case AGGREGATION_TRANSITION ⇒ new AggregationTransition(sourceCapsule, targetSlot, condition, Filter(filtered: _*))
      case EXPLORATION_TRANSITION ⇒ new ExplorationTransition(sourceCapsule, targetSlot, condition, Filter(filtered: _*))
      case END_TRANSITION ⇒ new EndExplorationTransition(sourceCapsule, targetSlot, condition, Filter(filtered: _*))
      case _ ⇒ throw new UserBadDataError("No matching type between capsule " + sourceCapsule + " and " + targetSlot + ". The transition can not be built")
    }
  }
}
