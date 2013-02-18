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

import algorithm.GA.{ GAAggregation, GAProfile }
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.implementation.data._
import org.openmole.misc.tools.io.FileUtil._
import fr.iscpif.mgo._
import java.io.File
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.service.Scaling._
import org.openmole.misc.tools.script.GroovyProxyPool

sealed class SaveProfileHook(
    val individual: Prototype[Individual[algorithm.GA#G, algorithm.GA#P, algorithm.GA#F]],
    val x: String,
    val aggregation: GAAggregation,
    val path: String) extends Hook {

  override def inputs = DataSet(individual.toArray)

  @transient lazy val xInterpreter = new GroovyProxyPool(x)

  def process(context: Context) {
    val file = new File(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(individual.toArray)
        xV = xInterpreter.execute(i.phenotype)
      } w.write("" + xV + "," + aggregation.aggregate(i.fitness) + "\n")
    }
  }

}
