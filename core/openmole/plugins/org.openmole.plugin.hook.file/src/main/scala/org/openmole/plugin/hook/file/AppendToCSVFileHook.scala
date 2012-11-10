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

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.mole._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.Prettifier._
import scala.annotation.tailrec

class AppendToCSVFileHook(
    fileName: String,
    prototypes: Prototype[_]*) extends Hook {

  override def process(moleJob: IMoleJob) = {
    import moleJob.context
    val file = new File(VariableExpansion(context, fileName))
    file.createParentDir

    val ps =
      if (prototypes.isEmpty) context.values.map { _.prototype }
      else prototypes

    val fos = new FileOutputStream(file, true)
    val bfos = new BufferedOutputStream(fos)
    try {
      val lock = fos.getChannel.lock
      try {
        if (file.size == 0) fos.appendLine(ps.map { _.name }.mkString(","))

        val lists = ps.map {
          p ⇒
            context.value(p) match {
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
          bfos.appendLine(list.map(l ⇒ l.prettify()).mkString(","))

        write(lists)
      } finally lock.release
    } finally bfos.close

  }

  override def requiered = DataSet(prototypes)

}