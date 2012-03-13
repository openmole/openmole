/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.implementation.validation

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.transition.ExplorationTransition
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.data._
import DataflowProblem._
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ValidationSpec extends FlatSpec with ShouldMatchers {
  
  "Validation" should "detect a missing input error" in {
    val p = new Prototype("t", classOf[String])
    
    val t1 = new EmptyTask("t1")
    val t2 = new EmptyTask("t2")
    t2.addInput(p)
    
    val c1 = new Capsule(t1)
    val c2 = new Capsule(t2)
    
    new Transition(c1, c2)
    
    val errors = Validation.typeErrors(new Mole(c1))
    errors.headOption match {
      case Some(MissingInput(_,_,d)) => assert(d.prototype == p)
      case None => sys.error("Error should have been detected")
    }
  }
  
  "Validation" should "not detect a missing input error" in {
    val p = new Prototype("t", classOf[String])
    
    val t1 = new EmptyTask("t1")
    val t2 = new EmptyTask("t2")
    t2.addInput(p)
    t2.addParameter(p, "Test")
    
    val c1 = new Capsule(t1)
    val c2 = new Capsule(t2)
    
    new Transition(c1, c2)
    
    Validation.typeErrors(new Mole(c1)).isEmpty should equal (true)
  }
  
  "Validation" should "detect a type error" in {
    val pInt = new Prototype("t", classOf[Int])
    val pString = new Prototype("t", classOf[String])
    
    val t1 = new EmptyTask("t1")
    t1.addOutput(pInt)
    val t2 = new EmptyTask("t2")
    t2.addInput(pString)
    
    val c1 = new Capsule(t1)
    val c2 = new Capsule(t2)
    
    new Transition(c1, c2)
    
    val errors = Validation.typeErrors(new Mole(c1))
    errors.headOption match {
      case Some(WrongType(_,_,d,t)) => 
        assert(d.prototype == pString)
        assert(t == pInt)
      case None => sys.error("Error should have been detected")
    }
  }
  
  "Validation" should "detect a topology error" in {
    val p = new Prototype("t", classOf[String])
    
    val t1 = new EmptyTask("t1")
    val t2 = new EmptyTask("t2")
    
    val c1 = new Capsule(t1)
    val c2 = new Capsule(t2)
    
    new ExplorationTransition(c1, c2)
    new Transition(c2, c1)
    
    val errors = Validation.topologyErrors(new Mole(c1))
    errors.isEmpty should equal (false) 
  }
  
  
  "Validation" should "detect a duplicated transition" in {
    val p = new Prototype("t", classOf[String])
    
    val t1 = new EmptyTask("t1")
    val t2 = new EmptyTask("t2")
    
    val c1 = new Capsule(t1)
    val c2 = new Capsule(t2)
    
    new Transition(c1, c2)
    new Transition(c1, c2)
    
    val errors = Validation.duplicatedTransitions(new Mole(c1))
    errors.isEmpty should equal (false) 
  }
}
