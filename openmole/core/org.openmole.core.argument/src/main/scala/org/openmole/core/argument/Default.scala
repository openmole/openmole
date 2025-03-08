package org.openmole.core.argument

import org.openmole.core.context.{Val, Variable}

/*
 * Copyright (C) 2022 Romain Reuillon
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

/**
 * The parameter is a variable which is injected in the data flow during the
 * workflow execution just before the beginning of a task execution. It can be
 * useful for testing purposes and for defining default value of inputs of a
 * task.
 *
 * @param prototype  prototype taking the value by default
 * @param value      value by default from the context
 * @param `override` should the default value override conflicting variable values
 * @tparam T type of the Val
 */
case class Default[T](prototype: Val[T], value: FromContext[T], `override`: Boolean):
  def toVariable = value.map(v => Variable(prototype, v))
