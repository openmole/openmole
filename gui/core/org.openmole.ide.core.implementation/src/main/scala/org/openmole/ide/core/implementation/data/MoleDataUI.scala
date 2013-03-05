/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.data

import org.openmole.ide.core.implementation.panel.MolePanelUI
import org.openmole.ide.core.model.data.IMoleDataUI
import org.openmole.ide.core.implementation.builder.MoleFactory

class MoleDataUI(val plugins: List[String] = List.empty) extends IMoleDataUI {

  def buildPanelUI = new MolePanelUI(this)

}
