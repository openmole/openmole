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

package org.openmole.core.workflow.validation

import org.openmole.core.context.Val
import org.openmole.core.setter._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation.TypeUtil.{ InvalidType, ValidType }
import org.scalatest._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.test.Stubs._

class TypeUtilSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "To array finder" should "not detect a toArray case" in {
    val p = Val[Int]("p")

    val t1 = EmptyTask() set (outputs += p)
    val t2 = EmptyTask() set (inputs += p)

    val mole: Mole = (t1 -- t2)

    val types = TypeUtil.computeTypes(mole, Sources.empty, Hooks.empty)(mole.slots(mole.capsules.find(_._task == t2).get).head)

    types.collect { case x: InvalidType => x }.isEmpty should equal(true)

    val validTypes = types.collect { case x: ValidType => x }
    validTypes.filter(_.toArray).isEmpty should equal(true)
    val tc = validTypes.filter(_.name == p.name).head
    tc.toArray should equal(false)
  }

  "To array finder" should "detect a toArray case" in {
    val p = Val[Int]("p")

    val t1 = EmptyTask() set (outputs += p)
    val t2 = EmptyTask() set (outputs += p)
    val t3 = EmptyTask() set (inputs += p)

    val mole: Mole = (t1 -- t3) & (t2 -- t3)

    val types = TypeUtil.computeTypes(mole, Sources.empty, Hooks.empty)(mole.slots(mole.capsules.find(_._task == t3).get).head)

    types.collect { case x: InvalidType => x }.isEmpty should equal(true)
    val validTypes = types.collect { case x: ValidType => x }
    val m = validTypes.filter(_.name == p.name).head
    m.toArray should equal(true)
    m.`type`.runtimeClass should equal(classOf[Int])
  }

}
