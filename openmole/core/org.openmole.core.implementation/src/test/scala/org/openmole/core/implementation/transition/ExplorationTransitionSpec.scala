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
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.sampling.ISampling
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class ExplorationTransitionSpec extends FlatSpec with ShouldMatchers {

  "Exploration transition" should "submit one MoleJob for each value in the sampling" in {
     
    val data = List("A","B","C")
    val i = new Prototype("i", classOf[String])
     
    val sampling = new ISampling {
      override def build(context: IContext) = data.map{v => List(new Variable(i, v))}
    }
    
    val exc = new ExplorationCapsule(new ExplorationTask("Exploration", sampling))
     
    val res = new ListBuffer[String]
    
    val t = new Task("Test") {
      override def process(context: IContext, progress: IProgress) = synchronized {
        context.contains(i) should equal (true)
        res += context.value(i).get
      }
    }
    
    t.addInput(i)
    
    new ExplorationTransition(exc, new Capsule(t))
                              
    new MoleExecution(new Mole(exc)).start.waitUntilEnded
    
    res.toArray.sorted.deep should equal (data.toArray.deep)
  }
}
