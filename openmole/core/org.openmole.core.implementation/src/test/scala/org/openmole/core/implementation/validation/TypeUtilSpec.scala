/*
 * Copyright (C) 2011 reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.validation

import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.{Task, ExplorationTask}
import org.openmole.core.implementation.sampling.EmptySampling
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.data.{Prototype, DataChannel}
import org.openmole.core.implementation.transition.{Transition, ExplorationTransition, AggregationTransition}
import org.openmole.core.model.data.IContext

import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TypeUtilSpec extends FlatSpec with ShouldMatchers {

  "To array finder" should "not detect a toArray case" in {
    val p = new Prototype("p", classOf[java.lang.Integer])
    
    val t1 = new EmptyTask("T1")
    t1.addOutput(p)
    
    val t2 = new EmptyTask("T2")
    t2.addInput(p)
    
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    
    new Transition(t1c, t2c)
    
    val manifests = TypeUtil.computeManifests(t2c.defaultInputSlot)
    
    manifests.filter(_.toArray).isEmpty should equal (true)
    val tc = manifests.filter(_.name == p.name).head
    tc.toArray should equal (false)
  }
  
  "To array finder" should "detect a toArray case" in {
    val p = new Prototype("p", classOf[Int])
    
    val t1 = new EmptyTask("T1")
    t1.addOutput(p)
    
    val t2 = new EmptyTask("T2")
    t2.addOutput(p)
    
    val t3 = new EmptyTask("T3")
    t3.addInput(p)
    
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = new Capsule(t3)
    
    new Transition(t1c, t3c)
    new Transition(t2c, t3c)

    val manifests = TypeUtil.computeManifests(t3c.defaultInputSlot)
    val m = manifests.filter(_.name == p.name).head
    m.toArray should equal (true)
    m.manifest.erasure should equal (classOf[Int])
  }
  
  "Type system" should "detect an toArray case when a data channel is going from a level to a lower level" in {      
    val i = new Prototype("i", classOf[String])

    val exc = new Capsule(new ExplorationTask("Exploration",  new EmptySampling))
     
    val testT = new EmptyTask("Test")
    testT.addOutput(i)
        
    val noOP = new EmptyTask("NoOP") 
    val aggT = new EmptyTask("Aggregation") 
    
    val testC = new Capsule(testT)
    val noOPC = new Capsule(noOP)
    val aggC = new Capsule(aggT)

    
    new ExplorationTransition(exc, testC)
    new Transition(testC, noOPC)
    new AggregationTransition(noOPC, aggC)
    
    new DataChannel(testC, aggC, i)
    
    val m = TypeUtil.computeManifests(aggC.defaultInputSlot).head
    m.toArray should equal(true)             
    m.manifest.erasure should equal (classOf[String])
  }
  
}
