/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.domain.relative

import java.lang.Double
import org.openmole.core.implementation.tools.VariableExpansion._
import org.openmole.core.model.data.IContext

class DoubleRelative(val nominal: String, val percent: String, val size: String) extends IRelative[Double] {

  override def  computeValues(context: IContext): Iterable[Double] = {
    val nom = expandData(context, nominal).toDouble
    val pe = expandData(context, percent).toDouble
    val s = expandData(context, size).toInt

    val min = nom * (1 - pe / 100.)
    if (s > 1) {
      val step = 2 * nom * pe / 100. / (s - 1)
      for (i <- 0 to s) yield new Double(min + i * s)
    } else {
      List(min,nom,nom * (1 + pe / 100.)).map{new Double(_)}
    }
  }
}
