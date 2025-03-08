/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.file

import java.io.File
import monocle.{Focus, Lens}
import org.openmole.core.context.{Context, Val, Variable}
import org.openmole.core.argument.FromContext
import org.openmole.core.setter.*
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.hook.{Hook, HookExecutionContext}
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.validation.*
import org.openmole.plugin.hook.file.CopyFileHook.*
import org.openmole.tool.random.*
import org.openmole.tool.archive.*

object CopyFileHook {

  case class CopyOptions(remove: Boolean, compress: Boolean, move: Boolean)

  trait CopyFileHookBuilder[T] {
    def copies: Lens[T, Vector[(Val[File], FromContext[File], CopyOptions)]]
  }

  implicit def isIO: InputOutputBuilder[CopyFileHook] = InputOutputBuilder(Focus[CopyFileHook](_.config))
  implicit def isInfo: InfoBuilder[CopyFileHook] = InfoBuilder(Focus[CopyFileHook](_.info))

  def apply(
    prototype:   Val[File],
    destination: FromContext[File],
    remove:      Boolean           = false,
    compress:    Boolean           = false,
    move:        Boolean           = false
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): CopyFileHook = {
    new CopyFileHook(
      Vector((prototype, destination, CopyOptions(remove, compress, move))),
      config = InputOutputConfig(),
      info = InfoConfig()
    ) set (
      inputs += prototype,
      if(move) outputs += prototype else identity
    )
  }
}

case class CopyFileHook(
  copies: Vector[(Val[File], FromContext[File], CopyOptions)],
  config: InputOutputConfig,
  info:   InfoConfig
) extends Hook with ValidateHook {

  override def validate = copies.flatMap(_._2.validate)

  override protected def process(executionContext: HookExecutionContext) = FromContext { parameters =>
    import parameters._

    def copyFile(
      context:       Context,
      filePrototype: Val[File],
      destination:   FromContext[File],
      options:       CopyOptions
    ): Option[Variable[File]] = {
      val from = context(filePrototype)
      val to = destination.from(context)

      to.createParentDirectory
      val ret: Option[Variable[File]] =
        if (options.move) {
          from.realFile.move(to)
          Some(Variable(filePrototype, to))
        }
        else if (options.compress) {
          from.copyCompress(to)
          None
        }
        else {
          from.copy(to)
          None
        }

      if (options.remove) from.recursiveDelete
      ret
    }

    val moved = for ((p, d, options) ← copies) yield copyFile(context, p, d, options)
    context ++ moved.flatten
  }

}
