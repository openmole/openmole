/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.hook.file

import org.openmole.ide.misc.widget.{ PluginPanel, CSVChooseFileTextField }
import org.openmole.ide.plugin.misc.tools.MultiPrototypePanel
import swing.Label
import org.openmole.ide.core.implementation.dataproxy.Proxies
import java.awt.Dimension
import org.openmole.ide.core.implementation.panelsettings.HookPanelUI
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.URL
import java.util.{ Locale, ResourceBundle }

class AppendToCSVFileHookPanelUI(dataUI: AppendToCSVFileHookDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("") with HookPanelUI {

  val filePathTextField = new CSVChooseFileTextField(dataUI.fileName)

  val multi = new MultiPrototypePanel("",
    dataUI.toBeHooked.toList,
    Proxies.instance.prototypes.toList)

  contents += {
    multi.contents.insert(0, filePathTextField)
    multi.contents.insert(0, new Label("CSV file path"))
    multi.contents.insert(0, new Label {
      text = "<html><b>Append prototypes to file</b></html>"
    })
    multi.minimumSize = new Dimension(300, 150)
    multi
  }

  val components = List(("Prototypes", this))

  def saveContent(name: String) = new AppendToCSVFileHookDataUI(name,
    Proxies.check(multi.multiPrototypeCombo.content.map {
      _.comboValue
    }.flatten),
    filePathTextField.text)

  override lazy val help = new Helper(List(new URL(i18n.getString("appendHookPermalinkText"), i18n.getString("appendHookPermalink"))))
}