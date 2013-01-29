/*
 * Copyright (C) 2013 Mathieu Leclaire
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

package org.openmole.plugin.builder

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.sampling._
import org.openmole.core.model.task._

package object base {

  def exploration(
    name: String,
    model: Puzzle,
    sampling: Sampling)(implicit plugins: PluginSet): Puzzle =
    new Capsule(ExplorationTask(name + "Replication", sampling)) -< model

  def aggregation(
    name: String,
    model: Puzzle,
    sampling: Sampling,
    aggregationTarget: Puzzle)(implicit plugins: PluginSet) =
    exploration(name, model, sampling) >- aggregationTarget
}
