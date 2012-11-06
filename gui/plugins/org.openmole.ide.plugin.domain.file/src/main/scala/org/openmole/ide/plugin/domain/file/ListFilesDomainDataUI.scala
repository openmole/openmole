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
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.domain.file.ListFilesDomain
import org.openmole.ide.core.model.data.IDomainDataUI

object ListFilesDomainDataUI {
  def apply(d: SubDataUI[File]) = d match {
    case x: ListFilesDomainDataUI ⇒ x
    case _ ⇒ new ListFilesDomainDataUI
  }
}

class ListFilesDomainDataUI(val directoryPath: String = "",
                            val regexp: String = ".*",
                            val recursive: Boolean = false) extends SubDataUI[File] {
  override def name = "Multiple"

  def coreObject(prototype: IPrototypeDataProxyUI,
                 domain: Option[IDomainDataUI[_]]) = new ListFilesDomain(new File(directoryPath), regexp, recursive)

  def coreClass = classOf[ListFilesDomain]

  def preview = " in " + new File(directoryPath).getName

  def buildPanelUI(p: IPrototypeDataProxyUI) = new ListFilesDomainPanelUI(this)
}
