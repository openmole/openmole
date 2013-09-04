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

import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data._
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

object FlattenTask {

  def apply(name: String)(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒

      val toFlatten = ListBuffer[(Prototype[Array[Array[S]]], Prototype[Array[S]]) forSome { type S }]()

      def flatten[S](from: Prototype[Array[Array[S]]], to: Prototype[Array[S]]) = {
        toFlatten += (from -> to)
        addInput(from)
        addOutput(to)
        this
      }

      def toTask =
        new FlattenTask(name, toFlatten.toList) with builder.Built
    }

}

sealed abstract class FlattenTask(val name: String, val toFlatten: List[(Prototype[Array[Array[S]]], Prototype[Array[S]]) forSome { type S }]) extends Task {

  override def process(context: Context) =
    toFlatten.map { case (f, r) ⇒ Variable(r.asInstanceOf[Prototype[Any]], context(f).flatten.toArray[Any](ClassTag(r.fromArray.`type`.runtimeClass))) }

}
