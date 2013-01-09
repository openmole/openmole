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
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.misc.tools.io.FileUtil._
import java.io.File
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.misc.tools.service.Scaling._

//FIXME scala type system is not yet able to match the correct prototype (use a cast)
sealed class SaveMapHook(
    val archive: Prototype[_],
    val path: String,
    val xScale: (Double, Double, Int),
    val yScale: (Double, Double, Int)) extends Hook {

  override def required = DataSet(archive)

  def process(context: Context) {
    val (xMin, xMax, nbX) = xScale
    val (yMin, yMax, nbY) = yScale
    val a = context(archive).asInstanceOf[MapArchive#A]
    val file = new File(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        (l, x) ← a.values.zipWithIndex
        (e, y) ← l.zipWithIndex
        if !e.isPosInfinity
      } w.write("" + x.toDouble.scale(xMin, xMax, 0, nbX) + "," + y.toDouble.scale(yMin, yMax, 0, nbY) + "," + e + "\n")
    }

  }
}
