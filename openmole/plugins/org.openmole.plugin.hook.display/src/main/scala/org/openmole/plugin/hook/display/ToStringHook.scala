/*
 * Copyright (C) 2011 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.display

import java.io.PrintStream
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.mole._

import org.openmole.core.implementation.data._

import org.openmole.misc.tools.io.Prettifier._

class ToStringHook(out: PrintStream, prototypes: Prototype[_]*) extends IHook {

  def this(prototypes: Prototype[_]*) = this(System.out, prototypes: _*)

  override def process(moleJob: IMoleJob) = {
    import moleJob.context

    if (!prototypes.isEmpty)
      out.println(
        prototypes.map(p ⇒ p -> context.variable(p)).map {
          _ match {
            case (p, Some(v)) ⇒ v
            case (p, None) ⇒ p.name + " not found"
          }
        }.mkString(","))
    else out.println(context.values.mkString(", "))
  }

  override def requiered = DataSet(prototypes)

}
