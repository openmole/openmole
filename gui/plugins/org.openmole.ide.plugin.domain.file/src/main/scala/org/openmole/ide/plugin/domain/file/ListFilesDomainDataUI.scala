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

package org.openmole.ide.plugin.domain.file

import java.io.File
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.domain.file.ListFilesDomain

class ListFilesDomainDataUI(directoryPath: String = "",
                            val regexp: String = ".*") extends FileDomainDataUI(directoryPath) {
  def coreObject(prototype: IPrototypeDataProxyUI) = new ListFilesDomain(new File(directoryPath), regexp)

  def coreClass = classOf[ListFilesDomain]

  def preview = " in " + new File(directoryPath).getName

  def buildPanelUI(p: IPrototypeDataProxyUI) = new ListFilesDomainPanelUI(this)

  def buildPanelUI = buildPanelUI(new PrototypeDataProxyUI(GenericPrototypeDataUI[File], false))

  override def toString = "File list"
}
