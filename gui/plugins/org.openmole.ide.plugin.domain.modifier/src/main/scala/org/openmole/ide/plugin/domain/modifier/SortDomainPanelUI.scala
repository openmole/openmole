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

import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.{ URL, Helper, PluginPanel }
import scala.swing.Label
import java.util.{ Locale, ResourceBundle }

class SortDomainPanelUI(dataUI: SortDomainDataUI[_]) extends PluginPanel("") with IDomainPanelUI {
  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  contents += new Label("<html><i>No more information is required for this Domain</i></html>")

  def saveContent = {
    val classString = ModifierDomainDataUI.computeClassString(dataUI)
    SortDomainDataUI(classString, dataUI.previousDomain)
  }

  override lazy val help =
    new Helper(List(new URL(i18n.getString("sortPermalinkText"), i18n.getString("sortPermalink"))))
}