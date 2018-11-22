/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.tools

import org.openmole.core.context._
import org.openmole.core.expansion._
import cats.implicits._

/**
 * The parameter is a variable wich is injected in the data flow during the
 * workflow execution just before the begining of a task execution. It can be
 * useful for testing purposes and for defining default value of inputs of a
 * task.
 *
 */
case class Default[T](prototype: Val[T], value: FromContext[T], `override`: Boolean) {
  def toVariable = value.map(v ⇒ Variable(prototype, v))
}
