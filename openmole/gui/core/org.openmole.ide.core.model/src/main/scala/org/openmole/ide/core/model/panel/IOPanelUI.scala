/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.model.panel

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.{ ImplicitPrototype, OutputPrototype, InputPrototype, IDataUI }

trait IOPanelUI extends IPanelUI {

  def save(name: String,
           prototypesIn: List[IPrototypeDataProxyUI],
           inputParameters: scala.collection.mutable.Map[IPrototypeDataProxyUI, String],
           prototypesOut: List[IPrototypeDataProxyUI]): IDataUI with InputPrototype with OutputPrototype with ImplicitPrototype
}