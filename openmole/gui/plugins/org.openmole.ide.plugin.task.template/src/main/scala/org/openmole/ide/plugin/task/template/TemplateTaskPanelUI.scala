/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.plugin.task.template

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.misc.widget._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import scala.swing.{ Label, ComboBox }
import java.io.File
import org.openmole.ide.core.implementation.dataproxy.Proxies

class TemplateTaskPanelUI(pud: TemplateTaskDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends TaskPanelUI {

  val protoFiles = Proxies.instance.classPrototypes(classOf[File])

  val templateTextField = new ChooseFileTextField(pud.template,
    "Select a template file",
    "",
    "*")

  val protoCombo = new ComboBox(protoFiles)

  val components = List(("Settings", {
    if (protoFiles.isEmpty) {
      new Label("First define a File Prototype")
    }
    else new PluginPanel("wrap 4") {
      contents += new Label("Template File")
      contents += templateTextField
      contents += new Label("into ")
      contents += protoCombo
    }
  }))

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  def saveContent(name: String): TaskDataUI = new TemplateTaskDataUI(name,
    templateTextField.text,
    Option(protoCombo.selection.item))
}
