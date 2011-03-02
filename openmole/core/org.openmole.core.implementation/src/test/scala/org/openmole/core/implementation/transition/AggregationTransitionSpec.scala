/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.implementation.transition

import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.capsule.Capsule
import org.openmole.core.implementation.capsule.ExplorationCapsule
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.sampling.ExplicitSampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.sampling.ISampling
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class AggregationTransitionSpec extends FlatSpec with ShouldMatchers {

  "Aggregation transition" should "turn results of exploration into a array of values" in {
     
    val data = List("A","A","B","C")
    val i = new Prototype("i", classOf[String])
     
    val sampling = new ExplicitSampling(i, data)
    
    val exc = new ExplorationCapsule(new ExplorationTask("Exploration", sampling))
     
    val emptyT = new EmptyTask("Empty")
    emptyT.addInput(i)
    emptyT.addOutput(i)
    
    val emptyC = new Capsule(emptyT)
    
    val testT = new Task("Test") {
      override def process(context: IContext, progress: IProgress) = {
        context.contains(toArray(i)) should equal (true)
        context.value(toArray(i)).get.sorted.deep should equal (data.toArray.deep)
      }
    }
    
    testT.addInput(toArray(i))
    
    val testC = new Capsule(testT)
    
    new ExplorationTransition(exc, emptyC)
    new AggregationTransition(emptyC, testC)
                              
    new MoleExecution(new Mole(exc)).start.waitUntilEnded 
  }
}

