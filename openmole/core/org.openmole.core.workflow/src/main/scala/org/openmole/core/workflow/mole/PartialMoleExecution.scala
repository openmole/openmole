/*
 * Copyright (C) 10/06/13 Romain Reuillon
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

package org.openmole.core.workflow.mole

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.execution.local.LocalEnvironment
import org.openmole.core.workspace.Workspace

object PartialMoleExecution {
  def apply(
    mole: Mole,
    sources: Iterable[(Capsule, Source)] = Iterable.empty,
    hooks: Iterable[(Capsule, Hook)] = Iterable.empty,
    environments: Map[Capsule, Environment] = Map.empty,
    grouping: Map[Capsule, Grouping] = Map.empty,
    seed: Long = Workspace.newSeed,
    defaultEnvironment: Environment = LocalEnvironment.default): PartialMoleExecution = new PartialMoleExecution(
    mole,
    sources groupBy { case (c, _) ⇒ c } map { case (c, ss) ⇒ c -> ss.map(_._2) } withDefault { _ ⇒ List.empty },
    hooks groupBy { case (c, _) ⇒ c } map { case (c, hs) ⇒ c -> hs.map(_._2) } withDefault { _ ⇒ List.empty },
    environments,
    grouping,
    seed,
    defaultEnvironment)
}

class PartialMoleExecution(
    val mole: Mole,
    val sources: Sources,
    val hooks: Hooks,
    val environments: Map[Capsule, Environment],
    val grouping: Map[Capsule, Grouping],
    val seed: Long,
    val defaultEnvironment: Environment) {

  def toExecution(implicits: Context = Context.empty)(implicit executionContext: ExecutionContext = ExecutionContext.local) =
    new MoleExecution(mole,
      sources,
      hooks,
      environments,
      grouping,
      seed,
      defaultEnvironment)(implicits, executionContext)

}
