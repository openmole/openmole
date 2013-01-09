/*
 * Copyright (C) 08/01/13 Romain Reuillon
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

import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.implementation.data._
import org.openmole.misc.tools.io.FileUtil._
import fr.iscpif.mgo._
import java.io.File
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.misc.tools.service.Scaling._

class SaveProfileHook(
    val archive: Prototype[_],
    val path: String,
    val xScale: (Double, Double, Int)) extends Hook {

  override def required = DataSet(archive)

  def process(context: Context) {
    val (xMin, xMax, nbX) = xScale
    val a = context(archive).asInstanceOf[ProfileArchive#A]
    val file = new File(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        (e, x) ← a.values.zipWithIndex
        if !e.isPosInfinity
      } w.write("" + x.toDouble.scale(xMin, xMax, 0, nbX) + "," + e + "\n")
    }

  }
}
