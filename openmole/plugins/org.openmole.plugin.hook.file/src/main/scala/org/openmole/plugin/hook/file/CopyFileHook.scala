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

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Prototype, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole.{ MoleExecutionContext, _ }
import org.openmole.core.workflow.validation.ValidateHook
import org.openmole.plugin.hook.file.CopyFileHook.CopyOptions
import org.openmole.tool.random.RandomProvider
import org.openmole.tool.tar._

object CopyFileHook {

  case class CopyOptions(remove: Boolean, compress: Boolean, move: Boolean)

  trait CopyFileHookBuilder[T] {
    def copies: Lens[T, Vector[(Prototype[File], FromContext[File], CopyOptions)]]
  }

  implicit def isIO: InputOutputBuilder[CopyFileHook] = InputOutputBuilder(CopyFileHook.config)

  implicit def isCopy: CopyFileHookBuilder[CopyFileHook] = new CopyFileHookBuilder[CopyFileHook] {
    override def copies = CopyFileHook.copies
  }

  def apply(
    prototype:   Prototype[File],
    destination: FromContext[File],
    remove:      Boolean           = false,
    compress:    Boolean           = false,
    move:        Boolean           = false
  ): CopyFileHook =
    apply() set (pack.copies += (prototype, destination, remove, compress, move))

  def apply(): CopyFileHook =
    new CopyFileHook(
      Vector.empty,
      config = InputOutputConfig()
    )

}

@Lenses case class CopyFileHook(
    copies: Vector[(Prototype[File], FromContext[File], CopyOptions)],
    config: InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Prototype[_]]) = copies.flatMap(_._2.validate(inputs)).toSeq

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val moved = for ((p, d, options) ← copies) yield copyFile(context, executionContext, p, d, options)
    context ++ moved.flatten
  }

  private def copyFile(
    context:          Context,
    executionContext: MoleExecutionContext,
    filePrototype:    Prototype[File],
    destination:      FromContext[File],
    options:          CopyOptions
  )(implicit rng: RandomProvider): Option[Variable[File]] = {
    val from = context(filePrototype)
    val to = destination.from(context)

    to.createParentDir
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

}
