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

package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object ToOffspringTask:

  def apply(evolution: EvolutionWorkflow)(using sourcecode.Name, DefinitionScope) =
    import evolution.integration.iManifest

    ClosureTask("ToOffspringTask") { (context, _, _) ⇒
      val i = evolution.buildIndividual(context(evolution.genomeVal), context, context(evolution.stateVal))
      Context(Variable(evolution.offspringPopulationVal, Array(i)))
    } set (
      inputs ++= evolution.outputVals,
      inputs += (evolution.genomeVal, evolution.stateVal),
      outputs += (evolution.stateVal, evolution.offspringPopulationVal)
    )


