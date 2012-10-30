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

package org.openmole.ide.plugin.environment.ssh

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

class SSHEnvironmentPanelUI(pud: SSHEnvironmentDataUI) extends PluginPanel("fillx,wrap 2", "", "") with IEnvironmentPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val loginTextField = new TextField(pud.login, 15)
  val hostTextField = new TextField(pud.host, 15)
  val nbSlotTextField = new TextField(pud.nbSlots.toString, 3)
  val dirTextField = new TextField(pud.dir, 15)
  val runTimeMemoryTextField = new TextField(pud.runtimeMemory.toString, 5)

  tabbedPane.pages += new TabbedPane.Page("Settings", new PluginPanel("wrap 2") {
    contents += (new Label("Login"), "gap para")
    contents += loginTextField

    contents += (new Label("Host"), "gap para")
    contents += hostTextField

    contents += (new Label("Number of slots"), "gap para")
    contents += nbSlotTextField

    contents += (new Label("Directory"), "gap para")
    contents += dirTextField
  })

  tabbedPane.pages += new TabbedPane.Page("Memory", new PluginPanel("wrap 2") {
    contents += (new Label("Runtime memory"), "gap para")
    contents += runTimeMemoryTextField
  })

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(loginTextField,
      new Help(i18n.getString("login"),
        i18n.getString("loginEx")))
    add(hostTextField,
      new Help(i18n.getString("host"),
        i18n.getString("hostEx")))
    add(nbSlotTextField,
      new Help(i18n.getString("nbSlot"),
        i18n.getString("nbSlotEx")))
    add(dirTextField,
      new Help(i18n.getString("dir"),
        i18n.getString("dirEx")))
    add(runTimeMemoryTextField,
      new Help(i18n.getString("runtimeMemory"),
        i18n.getString("runtimeMemoryEx")))

  }

  override def saveContent(name: String) = new SSHEnvironmentDataUI(name,
    loginTextField.text,
    hostTextField.text,
    nbSlotTextField.text.toInt,
    dirTextField.text,
    runTimeMemoryTextField.text.toInt)
}
