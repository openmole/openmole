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
import org.openmole.core.implementation.hook.CapsuleExecutionHook
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.Prettifier._
import scala.annotation.tailrec

class AppendToCSVFileHook(
    moleExecution: IMoleExecution,
    capsule: ICapsule,
    fileName: String,
    prototypes: IPrototype[_]*) extends CapsuleExecutionHook(moleExecution, capsule) {

  override def process(moleJob: IMoleJob) = {
    import moleJob.context
    val file = new File(VariableExpansion(context, fileName))
    if (!file.getParentFile.exists) file.getParentFile.mkdirs
    if (!file.getParentFile.isDirectory) throw new UserBadDataError("Cannot create directory " + file.getParentFile)

    val fos = new FileOutputStream(file, true)
    val bfos = new BufferedOutputStream(fos)
    try {
      val lock = fos.getChannel.lock
      try {
        if (file.size == 0) fos.appendLine(prototypes.map { _.name }.mkString(","))

        val lists = prototypes.map {
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

        def writeLine(list: List[_]) =
          bfos.appendLine(list.map { _.prettify }.mkString(","))

        write(lists)
      } finally lock.release
    } finally bfos.close

  }

  def inputs = DataSet(prototypes)

}