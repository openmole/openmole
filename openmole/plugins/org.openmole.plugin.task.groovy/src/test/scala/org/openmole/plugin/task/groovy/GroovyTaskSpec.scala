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

package org.openmole.plugin.task.groovy

import org.openmole.core.model.data.IContext
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class GroovyTaskSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "GroovyTask" should "run a groovy code" in {
    val p1 = new Prototype[Int]("p1")

    val groovyTask = GroovyTask("GroovyTask", "p1 *= 2")
    groovyTask addOutput p1

    val ctx = Context.empty + (p1 -> 2)

    groovyTask.toTask.process(ctx).value(p1).get should equal(4)
  }

  "GroovyTask" should "allow importing namespace" in {
    val p1 = new Prototype[AtomicBoolean]("p1")

    val groovyTask = GroovyTask("GroovyTask", "p1 = new AtomicBoolean()")
    groovyTask addImport "java.util.concurrent.atomic.*"
    groovyTask addOutput p1

    val ctx = groovyTask.toTask.process(Context.empty)
    ctx.contains(p1) should equal(true)
  }
}
