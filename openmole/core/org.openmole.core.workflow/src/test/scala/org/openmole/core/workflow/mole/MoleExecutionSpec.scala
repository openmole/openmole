/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.mole

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.setter._
import org.scalatest._

import scala.collection.mutable.ListBuffer
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution.{ Environment, LocalEnvironment }
import org.openmole.core.workflow.grouping.Grouping
import org.openmole.core.workflow.test.TestTask
import org.openmole.tool.random.RandomProvider

class MoleExecutionSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.Stubs._

  class JobGroupingBy2Test extends Grouping:
    def apply(context: Context, groups: Iterable[(MoleJobGroup, Iterable[Job])])(implicit newGroup: NewGroup, randomProvider: RandomProvider): MoleJobGroup =
      groups.find { case (_, g) => g.size < 2 } match 
        case Some((mg, _)) => mg
        case None          => MoleJobGroup()


  "Grouping jobs" should "not impact a normal mole execution" in:
    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")

    val sampling = ExplicitSampling(i, data)
    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val testT =
      TestTask: context =>
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.toVector should equal(data.toVector)
        context
      .set (inputs += i.array)

    val ex = ExplorationTask(sampling) -< (emptyT by new JobGroupingBy2Test) >- testT

    ex.run

  it should "accept int for by grouping" in:
    val data = List.fill(10)("A")
    val i = Val[String]("i")

    val sampling = ExplicitSampling(i, data)
    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val testT =
      TestTask: context =>
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.toVector should equal(data.toVector)
        context
      .set (inputs += i.array)

    val ex = ExplorationTask(sampling) -< (emptyT by 10) >- testT

    ex.run

  it should "by should group the mole jobs" in :
    import org.openmole.core.event.*

    val data = List.fill(50)("A")
    val i = Val[String]("i")

    val env = LocalEnvironment(1)

    val sampling = ExplicitSampling(i, data)
    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val testT =
      TestTask: context =>
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.toVector should equal(data.toVector)
        context
      .set(inputs += i.array)

    val ex =
      toMoleExecution:
        ExplorationTask(sampling) -< (emptyT by 10 on env) >- testT

    var nbJobs = 0

    ex.environments(env).listen:
      case (_, j: Environment.JobSubmitted) => nbJobs += 1

    ex.run

    nbJobs should equal(5)

  "Implicits" should "be used when input is missing" in:
    val i = Val[String]("i")
    val emptyT = EmptyTask() set (inputs += i)
    val emptyC = MoleCapsule(emptyT)
    MoleExecution(mole = Mole(emptyC), implicits = Context(Variable(i, "test"))).run

  "Wait" should "wait for the mole executon to be completed" in:
    val emptyT = EmptyTask()
    val me = emptyT.start(false)
    me.hangOn()

  "Delegation on environment" should "work" in:
    import org.openmole.core.event._

    @volatile var sub = 0

    val emptyT = EmptyTask()
    val env = LocalEnvironment(1)

    val mole = toMoleExecution(emptyT on env)

    mole.environments.head._2 listen:
      case (_, _: Environment.JobSubmitted) => sub += 1

    mole.run

    assert(sub == 1)

