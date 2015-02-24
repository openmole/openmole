/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow.builder

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.workflow.task._

object TaskBuilder {
  val nameCounter = new AtomicInteger
  def generateName(instance: Any) = {
    val instanceName = instance.getClass.getSuperclass.getSimpleName
    val name = instanceName.take(1).map(_.toLower) + instanceName.drop(1)
    name + TaskBuilder.nameCounter.getAndIncrement
  }

}

trait TaskBuilder extends InputOutputBuilder with Builder { builder ⇒
  def toTask: Task

  var name: Option[String] = None

  def setName(name: String): this.type = {
    builder.name = Some(name)
    this
  }

  trait Built extends super.Built {
    lazy val name = builder.name.getOrElse(TaskBuilder.generateName(this))
  }
}
