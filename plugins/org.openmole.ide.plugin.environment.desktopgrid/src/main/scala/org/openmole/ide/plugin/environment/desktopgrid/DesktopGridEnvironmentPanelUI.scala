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

package org.openmole.ide.plugin.environment.desktopgrid

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField

class DesktopGridEnvironmentPanelUI(pud: DesktopGridEnvironmentDataUI) extends PluginPanel("fillx,wrap 2", "[left][grow,fill]", "") with IEnvironmentPanelUI {
  val loginTextField = new TextField(20)
  val passTextField = new TextField(20)
  val portTextField = new TextField(5)

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  tabbedPane.pages += new TabbedPane.Page("Settings", new PluginPanel("wrap 2") {
    contents += (new Label("Login"), "gap para")
    contents += loginTextField
    contents += (new Label("Password"), "gap para")
    contents += passTextField
    contents += (new Label("Port"), "gap para")
    contents += portTextField
  })

  loginTextField.text = pud.login
  passTextField.text = pud.pass
  portTextField.text = pud.port.toString

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(loginTextField, new Help(i18n.getString("login"), i18n.getString("loginEx")))
    add(passTextField, new Help(i18n.getString("password"), i18n.getString("passwordEx")))
    add(portTextField, new Help(i18n.getString("port"), i18n.getString("portEx")))
  }

  override def saveContent(name: String) = new DesktopGridEnvironmentDataUI(name,
    loginTextField.text,
    passTextField.text,
    portTextField.text.toInt)
}
