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
package org.openmole.core.workflow.builder

import org.openmole.core.workflow.data._

trait OutputBuilder { builder ⇒
  private var _outputs = PrototypeSet.empty

  def addOutput(d: Prototype[_]*): this.type = { _outputs ++= d; this }

  def addExploredOutput(ds: Prototype[_ <: Array[_]]*): this.type = {
    for {
      d ← ds
    } {
      if (!_outputs.contains(d)) addOutput(d)
      _outputs = _outputs.explore(d.name)
    }
    this
  }

  def outputs = _outputs

  trait Built {
    def outputs = builder.outputs
  }

}
