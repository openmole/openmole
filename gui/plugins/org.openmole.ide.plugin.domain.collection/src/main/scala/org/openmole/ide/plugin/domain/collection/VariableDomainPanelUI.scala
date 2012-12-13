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

import org.openmole.ide.core.model.panel.IDomainPanelUI
import java.util.{ Locale, ResourceBundle }
import org.openmole.ide.misc.widget.{ Help, Helper, PluginPanel, URL }
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.misc.tools.util.Types._
import swing.MyComboBox

class VariableDomainPanelUI(dataUI: VariableDomainDataUI[_]) extends PluginPanel("wrap") with IDomainPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val availablePrototypes: List[IPrototypeDataProxyUI] =
    Proxys.prototypes.toList.filter {
      _.dataUI.dim == 1
    }

  val protoCombo = new MyComboBox(availablePrototypes)
  dataUI.prototypeArray match {
    case Some(p: IPrototypeDataProxyUI) ⇒ protoCombo.selection.item = p
    case _ ⇒
  }

  contents += protoCombo

  def saveContent = {
    val params = protoCombo.selection.item match {
      case p: IPrototypeDataProxyUI ⇒ (Some(p), p.dataUI.typeClassString)
      case _ ⇒ (None, DOUBLE)
    }
    VariableDomainDataUI(params._1, params._2)
  }

  override def help = new Helper(List(new URL(i18n.getString("variableDomainPermalink"),
    i18n.getString("variableDomainPermalinkText")))) {
    add(protoCombo, new Help(i18n.getString("prototypeArrayList")))
  }
}