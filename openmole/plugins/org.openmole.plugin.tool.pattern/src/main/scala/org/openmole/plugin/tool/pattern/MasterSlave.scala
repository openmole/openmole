/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.tool.pattern

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object MasterSlave:

  def apply(
    bootstrap: DSL,
    master:    Task,
    slave:     DSL,
    state:     Seq[Val[?]],
    slaves:    OptionalArgument[Int]       = None,
    stop:      OptionalArgument[Condition] = None
  )(implicit scope: DefinitionScope = "master slave"): DSL =
    val masterCapsule = Master(master, persist = state *)
    val masterSlaveLast = Strain(EmptyTask())

    val skel =
      stop.option match
        case Some(s) => bootstrap -< slave -- (masterCapsule >| masterSlaveLast when s)
        case None    => bootstrap -< slave -- masterCapsule


    val wf =
      skel &
        (masterCapsule -<- Slot(slave) slaves slaves) &
        (bootstrap oo masterCapsule keepAll state)

    DSLContainer(wf, (), output = Some(master))
