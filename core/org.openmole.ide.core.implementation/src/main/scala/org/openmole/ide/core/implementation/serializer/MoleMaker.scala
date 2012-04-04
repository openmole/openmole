/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.core.model.mole.IMole
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.Parameter
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.transition._
import org.openmole.ide.misc.tools.check.TypeCheck
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.ITask
import org.openmole.ide.core.implementation.data.AbstractExplorationTaskDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.ITransitionUI
import org.openmole.misc.tools.groovy.GroovyProxy
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet

object MoleMaker {
                                            
  def buildMoleExecution(mole: IMole,
                         manager: IMoleSceneManager, 
                         capsuleMap: Map[ICapsuleUI,ICapsule],
                         groupingStrategies: List[(IGroupingStrategy,ICapsule)]): (IMoleExecution,Iterable[(IEnvironment, String)]) = 
                           try{
      var envs = new HashSet[(IEnvironment,String)]
      val strat = new FixedEnvironmentSelection
      manager.capsules.values.foreach{c=> 
        c.dataUI.environment match {
          case Some(x : IEnvironmentDataProxyUI) => 
            try {
              val env = x.dataUI.coreObject
              envs += new Tuple2(env,x.dataUI.name)
              strat.select(capsuleMap(c),env)
            }catch {
              case e: UserBadDataError=> StatusBar.warn(e.message)
            }
          case _ =>
        }}
      val mgs = new MoleJobGrouping
      groupingStrategies.foreach(s=>mgs.set(s._2,s._1))
   
      (new MoleExecution(mole,strat,mgs),envs.toSet)
    }
  def buildMole(manager: IMoleSceneManager) = {
    if (manager.startingCapsule.isDefined){
      val prototypeMap: Map[IPrototypeDataProxyUI,IPrototype[_]] = Proxys.prototypes.map{p=> p->p.dataUI.coreObject}.toMap
      val capsuleMap : Map[ICapsuleUI,ICapsule] = manager.capsules.map{c=> c._2->buildCapsule(c._2.dataUI)}.toMap
      capsuleMap.foreach{case (cui,ccore)=> 
          manager.capsuleConnections(cui.dataUI).foreach(t=>buildTransition(ccore, capsuleMap(t.target.capsule),t))
          manager.dataChannels.filterNot{_.prototypes.isEmpty}.foreach{dc => new DataChannel(capsuleMap(dc.source),capsuleMap(dc.target),
                                                                                             dc.prototypes.map{p => prototypeMap(p)}.toArray)}}
      
      (new Mole(capsuleMap(manager.startingCapsule.get)),capsuleMap,prototypeMap)
    }
    else throw new UserBadDataError("No starting capsule is defined. The mole construction is not possible. Please define a capsule as a starting capsule.")  
  }
  
  def buildCapsule(capsuleDataUI: ICapsuleDataUI) = 
  capsuleDataUI.task match {
    case Some(x : ITaskDataProxyUI) => 
      x.dataUI match {
        case y : AbstractExplorationTaskDataUI => new Capsule(addPrototypes(capsuleDataUI,y.coreObject))
        case y : ITaskDataUI => new Capsule(addPrototypes(capsuleDataUI,y.coreObject))
      }
    case _ => new Capsule
  }
  
  def addPrototypes(capsuleDataUI: ICapsuleDataUI, task: ITask): ITask = {
    capsuleDataUI.task.get.dataUI.prototypesIn.foreach{case (pui,v)=> { 
          val proto = pui.dataUI.coreObject
          v.isEmpty match {
            case true=> task.addInput(proto)
            case false=>
              val groovyO = new GroovyProxy(v).execute()
              val (ok,msg) = TypeCheck(groovyO,proto)
              if (ok) task.addParameter(new Parameter(proto.asInstanceOf[IPrototype[Any]],groovyO))
              else throw new UserBadDataError(msg) 
          }
        }}
    capsuleDataUI.task.get.dataUI.prototypesOut.foreach{pui=> { task.addOutput(pui.dataUI.coreObject)}}
    task
  }
  
  def buildTransition(sourceCapsule: ICapsule, targetCapsule: ICapsule,t: ITransitionUI){
    t.transitionType match {
      case BASIC_TRANSITION=> new Transition(sourceCapsule,targetCapsule) 
      case AGGREGATION_TRANSITION=> new AggregationTransition(sourceCapsule,targetCapsule)
      case EXPLORATION_TRANSITION=> new ExplorationTransition(sourceCapsule,targetCapsule)
      case _=> throw new UserBadDataError("No matching type between capsule " + sourceCapsule +" and " + targetCapsule +". The transition can not be built")
    }
  }
}
