package org.openmole.gui.client.dataui

/*
 * Copyright (C) 20/08/14 // mathieu.leclaire@openmole.org
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

object DataUIs {
  val instance = new DataUIs
  def apply() = instance
}

class DataUIs {
  lazy val tasks: Var[List[Var[TaskDataUI]]] = Var(List())
  lazy val prototypes: Var[List[Var[PrototypeDataUI]]] = Var(List())
  lazy val domains: Var[List[Var[DomainDataUI]]] = Var(List())
}
