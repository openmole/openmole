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

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import org.openmole.core.context.Val
import org.openmole.core.argument.DefaultSet
import org.openmole.core.serializer.SerializerService
import org.openmole.core.setter._
import org.openmole.core.workflow.domain._
import org.scalatest._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.workflow.test.TestTask

class SerializationSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test._

  "Task " should "be the same after serialization and deserialization" in {
    val p = Val[Int]("p")

    val t = EmptyTask() set (
      inputs += p,
      outputs += p
    )

    val t2 = serializeDeserialize(t)
    t2.config.inputs.contains(p.name) should equal(true)
    t2.config.outputs.contains(p.name) should equal(true)
  }

  "Transient lazy field" should "not be null in defaultSet" in {
    val defaults = DefaultSet()
    defaults.defaultMap.get("test")
    val d2 = serializeDeserialize(defaults)
    defaults.defaultMap should equal(d2.defaultMap)
  }

  // -Ydelambdafy:inline makes it work on scala 2.13
  // inlining isSampling parameter makes it work on scala 3
  "Exploration " should "run after serialization and deserialization" in {
    val data = List("A", "B", "C")
    val i = Val[String]("i")

    implicit def listFactor: DiscreteFromContextDomain[List[String], String] = domain => Domain(domain.iterator)

    val exp = ExplorationTask(i in data)
    val t = EmptyTask() set (inputs += i)
    val t2 = serializeDeserialize(exp -< t)
    t2.run().hangOn()
  }

  "Compiled code" should "be detetected as such by the serializer" in {
    val interpreter = org.openmole.core.compiler.Interpreter()
    val i = interpreter.eval("""
    class InterpretedClass {
      def test = ""
    }
    new InterpretedClass()
    """)
    org.openmole.core.compiler.Interpreter.isInterpretedClass(i.getClass) should equal(true)

    class OpenMOLEClass
    org.openmole.core.compiler.Interpreter.isInterpretedClass(classOf[OpenMOLEClass]) should equal(false)
  }

}
