/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.hook.display

import org.openmole.ide.plugin.misc.tools.MultiPrototypePanel
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.panelsettings.HookPanelUI
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.URL
import java.util.{ Locale, ResourceBundle }

class ToStringHookPanelUI(dataUI: ToStringHookDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("") with HookPanelUI {

  val combo = new MultiPrototypePanel("Display prototypes",
    dataUI.toBeHooked,
    Proxies.instance.prototypes.toList)

  contents += combo

  val components = List(("Prototypes", this))

  def saveContent(name: String) = new ToStringHookDataUI(name,
    Proxies.check(combo.multiPrototypeCombo.content.map {
      _.comboValue
    }.flatten))

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))
}