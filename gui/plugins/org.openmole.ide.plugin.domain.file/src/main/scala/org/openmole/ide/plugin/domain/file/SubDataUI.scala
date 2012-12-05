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

import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.misc.tools.util.Types.FILE
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.sampling.IFinite

trait SubDataUI extends IDomainDataUI with IFinite {
  def name = "File"

  override def availableTypes = List(FILE)

  def directoryPath: String

  override def isAcceptable(domain: IDomainDataUI) = {
    StatusBar.warn("A file domain can not modify another Domain")
    super.isAcceptable(domain)
  }

  /*StatusBar.warn("A Discrete Domain is required as input of a Modifier Domain (Map, Take, Group, ...)")
  false

  override def isAcceptable(domain: IDomainDataUI) = {
    domain.
  }         */
}