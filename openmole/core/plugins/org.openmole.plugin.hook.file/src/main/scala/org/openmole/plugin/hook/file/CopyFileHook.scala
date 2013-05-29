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

import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.implementation.tools._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.mole._
import org.openmole.core.model.mole.ExecutionContext
import collection.mutable.ListBuffer

object CopyFileHook {

  trait CopyFileHookBuilder extends HookBuilder {
    def copy(prototype: Prototype[File], destination: String, remove: Boolean = false, compress: Boolean = false)
  }

  def apply(
    prototype: Prototype[File],
    destination: String,
    remove: Boolean = false,
    compress: Boolean = false): CopyFileHookBuilder = {
    val builder = apply()
    builder copy (prototype, destination, remove, compress)
    builder
  }

  def apply(): CopyFileHookBuilder =
    new CopyFileHookBuilder { hook ⇒
      private val copy = ListBuffer[(Prototype[File], String, Boolean, Boolean)]()

      def copy(prototype: Prototype[File], destination: String, remove: Boolean = false, compress: Boolean = false) = {
        copy += ((prototype, destination, remove, compress))
        addInput(prototype)
      }

      def toHook =
        new CopyFileHook with Built {
          val copy = hook.copy
        }
    }

}

abstract class CopyFileHook extends Hook {

  def copy: Iterable[(Prototype[File], String, Boolean, Boolean)]

  override def process(context: Context, executionContext: ExecutionContext) = {
    for ((p, d, r, c) ← copy) copy(context, executionContext, p, d, r, c)
    context
  }

  private def copy(
    context: Context,
    executionContext: ExecutionContext,
    filePrototype: Prototype[File],
    destination: String,
    remove: Boolean,
    compress: Boolean) = {
    val from = context(filePrototype)
    val to = executionContext.directory.child(new File(VariableExpansion(context, destination)))

    to.getParentFile.mkdirs
    if (compress) from.copyCompress(to)
    else from.copy(to)

    if (remove) from.recursiveDelete
  }

}
