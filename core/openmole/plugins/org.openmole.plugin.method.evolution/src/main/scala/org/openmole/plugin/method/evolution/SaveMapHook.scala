/*
 * Copyright (C) 23/11/12 Romain Reuillon
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

package org.openmole.plugin.method.evolution

import org.openmole.core.model.mole.Hook
import org.openmole.core.model.job.IMoleJob
import fr.iscpif.mgo._
import archive.MapArchive.MapElement
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.misc.tools.io.FileUtil._
import java.io.File
import org.openmole.core.implementation.tools.VariableExpansion

//FIXME scala type system is not yet able to match the correct prototype (use a cast)
sealed class SaveMapHook(val archive: Prototype[_], val path: String) extends Hook {

  override def required = DataSet(archive)

  def process(moleJob: IMoleJob) {
    val a = moleJob.context.valueOrException(archive).asInstanceOf[MapArchive#A]
    val file = new File(VariableExpansion(moleJob.context, path))
    file.createParentDir
    file.withWriter { w =>
      a.foreach {
        case((x, y), MapElement(value, _)) => w.write(s"$x,$y,$value\n")
      }
    }

  }
}
