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

package org.openmole.ui.plugin.builder

import org.openmole.ui.plugin.builder.Builder._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.mole.{Mole, MoleExecution, Capsule}
import org.openmole.core.implementation.data.Context
import org.openmole.core.model.data.IContext
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/*@RunWith(classOf[JUnitRunner])
class BuilderSpec extends FlatSpec with ShouldMatchers {
  "An iteration on capsule" should "be constructed" in {
    var nbIteration = 0
    
    val task = new Task("Test iteration") {
      override def process(context: IContext) = {
        nbIteration += 1; context
      }
    }
    
    val capsule = new Capsule(task)
    val it = iterative("testIteration", 10, capsule)
    
    new MoleExecution(new Mole(it.firstCapsule)).start.waitUntilEnded
  }
}
*/