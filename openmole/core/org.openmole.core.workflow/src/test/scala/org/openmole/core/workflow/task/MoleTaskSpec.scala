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

import org.openmole.core.context.Val
import org.openmole.core.exception.InternalProcessingError
import org.scalatest._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.setter._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.workflow.test.TestTask

import scala.util.Try

class MoleTaskSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "Implicits" should "work with mole task" in {
    val i = Val[String]
    val emptyT = EmptyTask() set (inputs += i)

    val moleTask =
      MoleTask(emptyT) set (
        implicits += i,
        i := "test"
      )

    moleTask run
  }

  "MoleTask" should "propagate errors" in {
    val error =
      TestTask { _ => throw new InternalProcessingError("Some error for test") } set (
        name := "error"
      )

    val moleTask = MoleTask(error)
    val res = Try { moleTask.run }

    res shouldBe a[util.Failure[_]]

  }

  "MoleTask" should "provide its inputs to the first capsule if it is a strainer" in {
    @volatile var executed = false

    val i = Val[String]

    val emptyT = TestTask { context =>
      executed = true
      context
    } set (inputs += i)

    val moleTask = MoleTask(Strain(EmptyTask()) -- emptyT) set (
      inputs += i,
      i := "test"
    )

    moleTask run

    executed should equal(true)
  }

  "MoleTask" should "work with the end exploration transition" in {
    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")

    val sampling = ExplicitSampling(i, data)

    val emptyT = EmptyTask() set ((inputs, outputs) += i)
    val testT = EmptyTask() set ((inputs, outputs) += i)

    val mt = MoleTask(sampling -< (emptyT >| Strain(testT) when "true"))

    val ex = mt -- testT

    ex.run
  }

}
