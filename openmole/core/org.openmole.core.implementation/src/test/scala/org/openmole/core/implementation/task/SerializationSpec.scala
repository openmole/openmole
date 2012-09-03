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

package org.openmole.core.implementation.task

import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.tools.io.BufferInputStream
import org.openmole.misc.tools.io.BufferOutputStream
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SerializationSpec extends FlatSpec with ShouldMatchers {
  "Task " should "be the same after serialization and deserialization" in {
    val p = Prototype[Int]("p")

    val t = EmptyTask("Test")
    t.addInput(p)
    t.addOutput(p)

    val builder = new BufferOutputStream

    SerializerService.serialize(t.toTask, builder)
    val t2 = SerializerService.deserialize[EmptyTask](new BufferInputStream(builder.buffer))

    t2.inputs.contains(p.name) should equal(true)
    t2.outputs.contains(p.name) should equal(true)
  }
}
