package org.openmole.gui.ext.dataproxy

import org.openmole.gui.ext.data.DataUI
import org.openmole.gui.tools.utils.ID
import rx.core.Var
/*
 * Copyright (C) 08/08/14 // mathieu.leclaire@openmole.org
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

trait DataProxy extends ID {

  type DATAUI <: DataUI

  val dataUI: Var[DATAUI]

  def generated: Boolean

  override def toString = dataUI.name

}