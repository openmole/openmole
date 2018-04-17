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

package org.openmole.plugin.hook.display

import java.io.PrintStream

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.mole._

object ToStringHook {

  implicit def isIO: InputOutputBuilder[ToStringHook] = InputOutputBuilder(ToStringHook.config)
  implicit def isInfo = InfoBuilder(info)

  def apply(prototypes: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): ToStringHook =
    apply(System.out, prototypes: _*)

  def apply(out: PrintStream, prototypes: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new ToStringHook(
      prototypes.toVector,
      config = InputOutputConfig(),
      info = InfoConfig()
    )

}

@Lenses case class ToStringHook(
  prototypes: Vector[Val[_]],
  config:     InputOutputConfig,
  info:       InfoConfig
) extends Hook {

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._
    if (!prototypes.isEmpty) {
      val filtered = Context(prototypes.flatMap(p ⇒ context.variable(p.asInstanceOf[Val[Any]])): _*)
      executionContext.services.outputRedirection.output.println(filtered.toString)
    }
    else executionContext.services.outputRedirection.output.println(context.toString)
    context
  }

}
