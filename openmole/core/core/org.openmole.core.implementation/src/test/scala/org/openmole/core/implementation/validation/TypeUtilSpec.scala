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

package org.openmole.core.implementation.validation

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.sampling._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.core.model.mole._
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TypeUtilSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "To array finder" should "not detect a toArray case" in {
    val p = Prototype[Int]("p")

    val t1 = EmptyTask("T1")
    t1 addOutput p

    val t2 = EmptyTask("T2")
    t2 addInput p

    val t1c = Capsule(t1)
    val t2c = Slot(t2)

    val mole = t1c -- t2c

    val manifests = TypeUtil.computeManifests(mole, Sources.empty, Hooks.empty)(t2c)

    manifests.filter(_.toArray).isEmpty should equal(true)
    val tc = manifests.filter(_.name == p.name).head
    tc.toArray should equal(false)
  }

  "To array finder" should "detect a toArray case" in {
    val p = Prototype[Int]("p")

    val t1 = EmptyTask("T1")
    t1 addOutput p

    val t2 = EmptyTask("T2")
    t2 addOutput p

    val t3 = EmptyTask("T3")
    t3 addInput p

    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = Slot(t3)

    val mole = (t1c -- t3c) + (t2c -- t3c)

    val manifests = TypeUtil.computeManifests(mole, Sources.empty, Hooks.empty)(t3c)
    val m = manifests.filter(_.name == p.name).head
    m.toArray should equal(true)
    m.manifest.runtimeClass should equal(classOf[Int])
  }

}
