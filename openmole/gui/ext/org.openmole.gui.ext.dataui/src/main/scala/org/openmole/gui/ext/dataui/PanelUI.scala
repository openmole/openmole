package org.openmole.gui.ext.dataui

import org.scalajs.dom.raw.HTMLElement

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

/*
 * Copyright (C) 26/11/14 // mathieu.leclaire@openmole.org
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

object PanelUI {
  def empty = new PanelUI {
    val view = div

    def save = {}
  }
}

trait PanelUI {
  def view: TypedTag[HTMLElement]
  def save: Unit
}
