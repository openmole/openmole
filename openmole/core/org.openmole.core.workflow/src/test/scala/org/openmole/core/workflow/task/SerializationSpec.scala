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

package org.openmole.core.workflow.task

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.openmole.core.context.Val
import org.openmole.core.workflow.data._
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.workflow.builder._
import org.scalatest._
import org.openmole.core.workflow.dsl._

class SerializationSpec extends FlatSpec with Matchers {
  "Task " should "be the same after serialization and deserialization" in {
    val p = Val[Int]("p")

    val t = EmptyTask() set (
      inputs += p,
      outputs += p
    )

    val builder = new ByteArrayOutputStream()

    SerialiserService.serialise(t, builder)
    val t2 = SerialiserService.deserialise[Task](new ByteArrayInputStream(builder.toByteArray))

    t2.config.inputs.contains(p.name) should equal(true)
    t2.config.outputs.contains(p.name) should equal(true)
  }
}
