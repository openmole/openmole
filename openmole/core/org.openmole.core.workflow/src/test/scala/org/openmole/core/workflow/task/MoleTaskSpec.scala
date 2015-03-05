/*
 * Copyright (C) 16/02/13 Romain Reuillon
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

package org.openmole.core.workflow.task

import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.scalatest._
import org.openmole.core.workflow.mole._

import scala.util.Try

class MoleTaskSpec extends FlatSpec with Matchers {

  "Implicits" should "work with mole task" in {
    val i = Prototype[String]("i")
    val emptyT = EmptyTask()
    emptyT.addInput(i)

    val emptyC = Capsule(emptyT)

    val moleTask =
      MoleTask(Mole(emptyC), emptyC)

    moleTask addImplicit i
    moleTask setDefault (i, "test")

    MoleExecution(Mole(moleTask)).start.waitUntilEnded
  }

  "MoleTask" should "propagate errors" in {

    val error = new TestTask {
      val name = "error"
      override def process(context: Context) =
        throw new InternalProcessingError("Some error for test")
    }
    val moleTask = MoleTask(error)

    val ex = moleTask.start
    Try { ex.waitUntilEnded }

    ex.exception shouldNot equal(None)

  }

}
