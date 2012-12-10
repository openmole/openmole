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
package org.openmole.ide.plugin.sampling.combine

import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.misc.widget.{ Help, PluginPanel }
import org.openmole.ide.core.implementation.dataproxy.Proxys
import swing.MyComboBox
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import java.util.{ Locale, ResourceBundle }

class ZipWithPrototypeSamplingPanelUI(dataUI: ZipWithPrototypeSamplingDataUI) extends PluginPanel("") with ISamplingPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val availablePrototypes: List[IPrototypeDataProxyUI] = {
    dataUI match {
      case i: ZipWithIndexSamplingDataUI ⇒ Proxys.classPrototypes(classOf[Int])
      case n: ZipWithNameSamplingDataUI ⇒ Proxys.classPrototypes(classOf[String])
      case _ ⇒ List()
    }
  }

  val protoCombo = new MyComboBox(availablePrototypes)
  dataUI.prototype match {
    case Some(p: IPrototypeDataProxyUI) ⇒ protoCombo.selection.item = p
    case _ ⇒
  }

  contents += protoCombo

  help.add(protoCombo, new Help(i18n.getString("zipPrototype")))

  def saveContent = dataUI match {
    case i: ZipWithIndexSamplingDataUI ⇒ new ZipWithIndexSamplingDataUI(proto)
    case n: ZipWithNameSamplingDataUI ⇒ new ZipWithNameSamplingDataUI(proto)
    case _ ⇒ new ZipWithIndexSamplingDataUI
  }

  def proto = protoCombo.selection.item match {
    case p: IPrototypeDataProxyUI ⇒ Some(p)
    case _ ⇒ None
  }
}