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

package org.openmole.ide.plugin.task.groovy

import java.awt.Dimension
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import scala.swing.FileChooser.SelectionMode._
import org.openmole.ide.misc.widget.GroovyEditor
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.TabbedPane

class GroovyTaskPanelUI(pud: GroovyTaskDataUI) extends PluginPanel("") with ITaskPanelUI {
  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val codeTextArea = new GroovyEditor {
    editor.text = pud.code
    minimumSize = new Dimension(40, 100);
    tooltip = Help.tooltip(i18n.getString("groovyCode"),
      i18n.getString("groovyCodeEx"))
  }

  val libMultiTextField = new MultiChooseFileTextField("Libraries",
    pud.libs.map { l ⇒ new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(l)) },
    "Select a file",
    Some("Lib files"),
    FilesOnly,
    Some("jar"),
    CLOSE_IF_EMPTY) {
    tooltip = Help.tooltip(i18n.getString("libraryPath"),
      i18n.getString("libraryPathEx"))
  }

  tabbedPane.pages += new TabbedPane.Page("Code", codeTextArea)

  tabbedPane.pages += new TabbedPane.Page("Library", libMultiTextField.panel)

  override def saveContent(name: String): ITaskDataUI = new GroovyTaskDataUI(name,
    codeTextArea.editor.text,
    libMultiTextField.content.map { _.content }.filterNot(_.isEmpty))
}
