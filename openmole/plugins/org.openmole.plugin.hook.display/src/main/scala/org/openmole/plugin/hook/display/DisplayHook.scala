/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.hook.display

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import java.io.PrintStream

object DisplayHook {

  def apply(toDisplay: FromContext[String])(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    Hook("DisplayHook") { parameters =>
      import parameters._
      outputRedirection.output.println(toDisplay.from(context))
      context
    } withValidate { toDisplay.validate }

  def apply(out: PrintStream, prototypes: Val[?]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): Hook =
    Hook("DisplayHook") { parameters =>
      import parameters._
      if (!prototypes.isEmpty) {
        val filtered = Context(prototypes.flatMap(p => context.variable(p.asInstanceOf[Val[Any]])) *)
        outputRedirection.output.println(filtered.toString)
      }
      else outputRedirection.output.println(context.toString)
      context
    }

  def apply(prototypes: Val[?]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): Hook = apply(System.out, prototypes *)

}
