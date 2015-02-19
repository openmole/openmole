package org.openmole.gui.client.core

import org.openmole.gui.client.core.dataui.DataBagUI

/*
 * Copyright (C) 11/02/15 // mathieu.leclaire@openmole.org
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

import rx._

class PanelSequences {
  private val sequences: Var[Seq[(DataBagUI, Int)]] = Var(Seq())

  def stack(dataBagUI: DataBagUI, index: Int) = sequences() = sequences() :+ (dataBagUI, index)

  def flush: Option[(DataBagUI, Int)] = {
    val last = sequences().lastOption
    last.map { l ⇒ sequences() = sequences().filterNot(_._1.uuid == l._1.uuid) }
    last
  }

  def isEmpty = sequences().isEmpty
}
