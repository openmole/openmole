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

import algorithm.GA.GAAggregation
import org.openmole.core.model.mole.Hook
import org.openmole.core.model.job.IMoleJob
import fr.iscpif.mgo._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.misc.tools.io.FileUtil._
import java.io.File
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.misc.tools.service.Scaling._
import org.openmole.misc.tools.script.GroovyProxyPool
import org.openmole.core.implementation.tools._

//FIXME scala type system is not yet able to match the correct prototype (use a cast)
sealed class SaveMapHook(
    val individual: Prototype[Individual[algorithm.GA#G, algorithm.GA#P, algorithm.GA#F]],
    val x: String,
    val y: String,
    val aggregation: GAAggregation,
    val path: String) extends Hook {

  override def inputs = DataSet(individual.toArray)

  @transient lazy val xInterpreter = new GroovyProxyPool(x)
  @transient lazy val yInterpreter = new GroovyProxyPool(y)

  def process(context: Context) {
    val file = new File(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(individual.toArray)
        xV = xInterpreter.execute(i.phenotype)
        yV = yInterpreter.execute(i.phenotype)
      } w.write("" + xV + "," + yV + "," + aggregation.aggregate(i.fitness) + "\n")
    }

  }
}
