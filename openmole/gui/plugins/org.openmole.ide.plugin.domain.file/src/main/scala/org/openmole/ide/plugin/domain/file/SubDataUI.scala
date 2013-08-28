/*
 * Copyright (C) 2012 Mathieu Leclaire 
 * < mathieu.leclaire at openmole.org >
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

package org.openmole.ide.plugin.domain.file

import org.openmole.ide.misc.tools.util.Types.FILE
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.data.DomainDataUI
import org.openmole.ide.core.implementation.sampling.FiniteUI

trait SubDataUI extends DomainDataUI with FiniteUI {
  def name = "File"

  override def availableTypes = List(FILE)

  def directoryPath: String

  override def isAcceptable(domain: DomainDataUI) = {
    StatusBar().warn("A file domain can not modify another Domain")
    super.isAcceptable(domain)
  }
}