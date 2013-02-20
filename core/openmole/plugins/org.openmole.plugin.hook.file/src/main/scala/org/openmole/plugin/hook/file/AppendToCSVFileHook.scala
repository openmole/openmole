/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.hook.file

import java.io.File
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.Prettifier._
import scala.annotation.tailrec
import org.openmole.core.implementation.mole._

object AppendToCSVFileHook {

  def apply(fileName: String, prototypes: Prototype[_]*) =
    new HookBuilder {
      prototypes.foreach(addInput(_))
      def toHook = new AppendToCSVFileHook(fileName, prototypes: _*) with Built
    }

}

abstract class AppendToCSVFileHook(
    fileName: String,
    prototypes: Prototype[_]*) extends Hook {

  override def process(context: Context) = {
    val file = new File(VariableExpansion(context, fileName))
    file.createParentDir

    val ps =
      if (prototypes.isEmpty) context.values.map { _.prototype }
      else prototypes

    file.withLock {
      fos ⇒
        if (file.size == 0) fos.appendLine(ps.map { _.name }.mkString(","))

        val lists = ps.map {
          p ⇒
            context.option(p) match {
              case Some(v) ⇒
                v match {
                  case v: Array[_] ⇒ v.toList
                  case v ⇒ List(v)
                }
              case None ⇒ List("not found")
            }
        }.toList

        @tailrec def write(lists: List[List[_]]): Unit =
          if (!lists.exists(_.size > 1)) writeLine(lists.map { _.head })
          else {
            writeLine(lists.map { _.head })
            write(lists.map { l ⇒ if (l.size > 1) l.tail else l })
          }

        def writeLine[T](list: List[T]) =
          fos.appendLine(list.map(l ⇒ l.prettify()).mkString(","))

        write(lists)
    }
    context
  }

}
