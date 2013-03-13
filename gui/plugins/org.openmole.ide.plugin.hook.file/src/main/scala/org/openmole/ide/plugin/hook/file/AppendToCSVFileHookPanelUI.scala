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

import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.misc.widget.CSVChooseFileTextField
import org.openmole.ide.plugin.misc.tools.MultiPrototypePanel
import swing.{ TabbedPane, Label }
import org.openmole.ide.core.implementation.dataproxy.Proxys

class AppendToCSVFileHookPanelUI(dataUI: AppendToCSVFileHookDataUI) extends MultiPrototypePanel("",
  dataUI.prototypes.toList,
  Proxys.prototypes.toList) with IHookPanelUI {

  val filePathTextField = new CSVChooseFileTextField(dataUI.fileName)
  contents.insert(0, filePathTextField)
  contents.insert(0, new Label("CSV file path"))
  contents.insert(0, new Label { text = "<html><b>Append prototypes to file</b></html>" })

  tabbedPane.pages.insert(0, new TabbedPane.Page("Prototypes", this))

  def saveContent(name: String) = new AppendToCSVFileHookDataUI(name,
    multiPrototypeCombo.content.map { _.comboValue.get }.toList,
    filePathTextField.text)
}