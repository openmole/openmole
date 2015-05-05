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

import org.openmole.plugin.hook.file.CopyFileHook.CopyOptions
import org.openmole.tool.file._
import org.openmole.tool.tar._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.mole.ExecutionContext
import collection.mutable.ListBuffer

object CopyFileHook {

  case class CopyOptions(remove: Boolean, compress: Boolean, move: Boolean)

  trait CopyFileHookBuilder extends HookBuilder {
    def addCopy(prototype: Prototype[File], destination: ExpandedString, remove: Boolean = false, compress: Boolean = false, move: Boolean = false)
  }

  def apply(
    prototype: Prototype[File],
    destination: ExpandedString,
    remove: Boolean = false,
    compress: Boolean = false,
    move: Boolean = false): CopyFileHookBuilder = {
    val builder = apply()
    builder addCopy (prototype, destination, remove, compress, move)
    builder
  }

  def apply(): CopyFileHookBuilder =
    new CopyFileHookBuilder { hook ⇒
      private val copy = ListBuffer[(Prototype[File], ExpandedString, CopyOptions)]()

      def addCopy(prototype: Prototype[File], destination: ExpandedString, remove: Boolean = false, compress: Boolean = false, move: Boolean = false) = {
        copy += ((prototype, destination, CopyOptions(remove, compress, move)))
        addInput(prototype)
      }

      def toHook =
        new CopyFileHook with Built {
          val copy = hook.copy
        }
    }

}

abstract class CopyFileHook extends Hook {

  def copy: Iterable[(Prototype[File], ExpandedString, CopyOptions)]

  override def process(context: Context, executionContext: ExecutionContext) = {
    for ((p, d, options) ← copy) copy(context, executionContext, p, d, options)
    context
  }

  private def copy(
    context: Context,
    executionContext: ExecutionContext,
    filePrototype: Prototype[File],
    destination: ExpandedString,
    options: CopyOptions) = {
    val from = context(filePrototype)
    val to = executionContext.relativise(destination.from(context))

    to.createParentDir
    if (options.move) from.realFile.move(to)
    else if (options.compress) from.copyCompress(to)
    else from.copyContent(to)

    if (options.remove) from.recursiveDelete
  }

}
