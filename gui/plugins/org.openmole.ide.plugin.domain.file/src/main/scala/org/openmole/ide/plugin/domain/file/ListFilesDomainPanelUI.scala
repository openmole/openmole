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

package org.openmole.ide.plugin.domain.file

import java.io.File
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label
import scala.swing.TextField

class ListFilesDomainPanelUI(dataUI: ListFilesDomainDataUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IDomainPanelUI {

  val directoryTextField = new ChooseFileTextField(dataUI.directoryPath)
  val regexpTextField = new TextField(8) { text = dataUI.regexp }

  val filePrototypes = Proxys.classPrototypes(classOf[File])
  contents += (new Label("List of files"), "gap para")
  contents += (directoryTextField, "wrap")

  contents += (new Label("Regular expression"), "gap para")
  contents += (regexpTextField, "wrap")

  def saveContent = new ListFilesDomainDataUI(directoryTextField.text,
    regexpTextField.text)

}