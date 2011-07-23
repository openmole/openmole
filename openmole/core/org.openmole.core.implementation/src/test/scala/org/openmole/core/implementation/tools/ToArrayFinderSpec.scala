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

package org.openmole.core.implementation.tools

import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.model.data.IContext

import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ToArrayFinderSpec extends FlatSpec with ShouldMatchers {

  "To array finder" should "not detect a toArray case" in {
    val p = new Prototype("p", classOf[java.lang.Integer])
    
    val t1 = new EmptyTask("T1")
    t1.addOutput(p)
    
    val t2 = new EmptyTask("T2")
    t2.addInput(p)
    
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    
    new Transition(t1c, t2c)
    
    val spannedToArray = TypeUtil.spanArrayManifests(t2c.intputSlots.head)
    
    spannedToArray._1.isEmpty should equal (true)
    spannedToArray._2.contains(p.name) should equal (true)
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

    val manifests = TypeUtil.spanArrayManifests(t3c.intputSlots.head)._1
    manifests.contains(p.name) should equal (true)
    manifests.get(p.name).get.erasure should equal (classOf[Int])
  }
}
