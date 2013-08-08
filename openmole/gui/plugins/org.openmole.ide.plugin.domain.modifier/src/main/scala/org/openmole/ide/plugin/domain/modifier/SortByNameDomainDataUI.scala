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

import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label
import java.io.File
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.domain.{ Finite, Domain }
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.misc.tools.util.Types.FILE
import org.openmole.plugin.domain.modifier.SortByNameDomain
import org.openmole.misc.tools.obj.ClassUtils
import util.Try
import org.openmole.ide.core.implementation.data.DomainDataUI
import org.openmole.ide.core.implementation.panelsettings.IDomainPanelUI
import org.openmole.ide.core.implementation.sampling.FiniteUI

class SortByNameDomainDataUI(var previousDomain: List[DomainDataUI] = List.empty)
    extends ModifierDomainDataUI with FiniteUI {

  def domainType = manifest[File]

  def coreObject = Try {
    previousDomain.headOption match {
      case Some(x: DomainDataUI) ⇒ SortByNameDomain(x.coreObject.asInstanceOf[Domain[File] with Finite[File]])
      case _                     ⇒ throw new UserBadDataError("The SortByName Domain requires a File Domain as input")
    }
  }

  def buildPanelUI = new IDomainPanelUI {
    val components = List(("", new PluginPanel("wrap ") {
      contents += new Label("<html><i>No more information is required for this Domain</i></html>")
    }))

    def saveContent = new SortByNameDomainDataUI
  }

  def preview = name

  override def name = "Sort by Name"

  def coreClass = classOf[SortByNameDomain]

  override def availableTypes = List(FILE)

  override def isAcceptable(domain: DomainDataUI) = {
    // if (Types(domain.domainType.toString, FILE)) true
    if (ClassUtils.assignable(domain.domainType.runtimeClass, classOf[File])) true
    else {
      StatusBar().warn("A file domain can not modify another Domain")
      false
    }
  }

  def clone(pD: List[DomainDataUI]) = pD.headOption match {
    case Some(d: DomainDataUI) ⇒ new SortByNameDomainDataUI(pD)
    case _                     ⇒ new SortByNameDomainDataUI(List())
  }
}