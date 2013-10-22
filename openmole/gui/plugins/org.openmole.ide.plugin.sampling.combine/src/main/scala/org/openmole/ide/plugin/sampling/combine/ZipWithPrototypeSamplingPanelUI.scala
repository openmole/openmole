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

import org.openmole.ide.misc.widget.{ ContentComboBox, Help, PluginPanel }
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import swing.MyComboBox
import java.util.{ Locale, ResourceBundle }
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.panelsettings.ISamplingPanelUI

class ZipWithPrototypeSamplingPanelUI(dataUI: ZipWithPrototypeSamplingDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends ISamplingPanelUI {

  val availablePrototypes: List[PrototypeDataProxyUI] = {
    dataUI match {
      case i: ZipWithIndexSamplingDataUI ⇒ Proxies.instance.classPrototypes(classOf[Int])
      case n: ZipWithNameSamplingDataUI  ⇒ Proxies.instance.classPrototypes(classOf[String])
      case _                             ⇒ List()
    }
  }

  val protoCombo = ContentComboBox(availablePrototypes, dataUI.prototype)

  val components = List(("", new PluginPanel("") {
    contents += protoCombo.widget
  }))

  add(protoCombo.widget, new Help(i18n.getString("zipPrototype")))

  def saveContent = dataUI match {
    case i: ZipWithIndexSamplingDataUI ⇒ new ZipWithIndexSamplingDataUI(proto)
    case n: ZipWithNameSamplingDataUI  ⇒ new ZipWithNameSamplingDataUI(proto)
    case _                             ⇒ throw new UserBadDataError("The data for the 'Zip with' Sampling is not correct ")
  }

  def proto = protoCombo.widget.selection.item.content
}