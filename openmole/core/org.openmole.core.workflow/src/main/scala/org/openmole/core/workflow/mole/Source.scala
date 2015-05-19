/*
 * Copyright (C) 17/02/13 Romain Reuillon
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

package org.openmole.core.workflow.mole

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._

object Source {
  implicit def sourceBuilderToSourceConverter(sb: SourceBuilder) = sb.toSource
}

trait Source <: InputOutputCheck {
  def inputs: PrototypeSet
  def outputs: PrototypeSet
  def defaults: DefaultSet
  protected def process(context: Context, executionContext: ExecutionContext)(implicit rng: RandomProvider): Context
  def perform(context: Context, executionContext: ExecutionContext)(implicit rng: RandomProvider): Context = perform(context, process(_, executionContext))
}