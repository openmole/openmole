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
package org.openmole.ide.plugin.domain.collection

import java.util.{ Locale, ResourceBundle }
import org.openmole.ide.misc.widget.{ Help, Helper, PluginPanel, URL }
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.ide.misc.tools.util.Types
import swing.MyComboBox
import org.openmole.ide.core.implementation.panelsettings.IDomainPanelUI

class VariableDomainPanelUI(dataUI: VariableDomainDataUI[_])(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends IDomainPanelUI {

  val availablePrototypes: List[PrototypeDataProxyUI] =
    Proxies.instance.prototypes.toList.filter {
      _.dataUI.dim == 1
    }

  val protoCombo = new MyComboBox(availablePrototypes)
  dataUI.prototypeArray match {
    case Some(p: PrototypeDataProxyUI) ⇒ protoCombo.selection.item = p
    case _                             ⇒
  }
  val components = List(("", new PluginPanel("wrap") {
    contents += protoCombo
  }))

  def saveContent = {
    val params = protoCombo.selection.item match {
      case p: PrototypeDataProxyUI ⇒ (Some(p), p.dataUI.typeClassString)
      case _                       ⇒ (None, DOUBLE)
    }
    VariableDomainDataUI(params._1, Types.pretify(params._2))
  }

  override lazy val help = new Helper(List(new URL(i18n.getString("variableDomainPermalink"), i18n.getString("variableDomainPermalinkText"))))

  add(protoCombo, new Help(i18n.getString("prototypeArrayList")))

}