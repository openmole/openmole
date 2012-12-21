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
package org.openmole.ide.plugin.domain.modifier

import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label
import java.io.File
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.domain.{ Finite, Domain }
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.misc.tools.util.Types.FILE
import org.openmole.ide.misc.tools.util.Types
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.plugin.domain.modifier.SortByNameDomain

class SortByNameDomainDataUI(var previousDomain: List[IDomainDataUI] = List.empty)
    extends IDomainDataUI with IFinite {

  def domainType = manifest[File]

  def coreObject = previousDomain.headOption match {
    case Some(x: IDomainDataUI) ⇒ new SortByNameDomain(x.coreObject.asInstanceOf[Domain[File] with Finite[File]])
    case _ ⇒ throw new UserBadDataError("The SortByName Domain requires a File Domain as input")
  }

  def buildPanelUI = new PluginPanel("") with IDomainPanelUI {
    contents += new Label("<html><i>No more information is required for this Domain</i></html>")

    def saveContent = new SortByNameDomainDataUI
  }

  def preview = name

  override def name = "Sort by Name"

  def coreClass = classOf[SortByNameDomain]

  override def availableTypes = List(FILE)

  override def isAcceptable(domain: IDomainDataUI) = {
    if (Types(domain.domainType.toString, FILE)) true
    else {
      StatusBar().warn("A file domain can not modify another Domain")
      false
    }
  }
}