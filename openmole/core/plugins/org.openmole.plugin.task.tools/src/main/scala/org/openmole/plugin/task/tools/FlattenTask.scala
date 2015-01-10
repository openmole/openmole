/*
 * Copyright (C) 03/09/13 Romain Reuillon
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

package org.openmole.plugin.task.tools

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

object FlattenTask {

  def apply[S](flatten: Prototype[Array[Array[S]]], in: Prototype[Array[S]]) =
    new TaskBuilder { builder ⇒ def toTask =
        new FlattenTask(flatten, in) with builder.Built
    }

}

sealed abstract class FlattenTask[S](val flatten: Prototype[Array[Array[S]]], val in: Prototype[Array[S]]) extends Task {

  override def process(context: Context) = {
    implicit val sClassTag = ClassTag[S](in.fromArray.`type`.runtimeClass)
    Variable(in, context(flatten).flatten.toArray[S])
  }

}
