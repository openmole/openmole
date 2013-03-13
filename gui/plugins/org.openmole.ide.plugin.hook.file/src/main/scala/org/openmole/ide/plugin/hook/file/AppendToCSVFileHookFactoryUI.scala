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

package org.openmole.ide.plugin.hook.file

import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.plugin.hook.file._

class AppendToCSVFileHookFactoryUI extends IHookFactoryUI {

  def buildDataUI = new AppendToCSVFileHookDataUI

  def coreClass = classOf[AppendToCSVFileHook]

  override def toString = "Append Prototype to file"

}
