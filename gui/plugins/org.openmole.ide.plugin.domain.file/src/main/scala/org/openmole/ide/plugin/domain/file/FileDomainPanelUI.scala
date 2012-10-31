package org.openmole.ide.plugin.domain.file

import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.PluginPanel
import swing.Label

/*
* Copyright (C) 2012 Mathieu Leclaire
* < mathieu.leclaire at openmole.org >
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

abstract class FileDomainPanelUI extends PluginPanel("fillx", "[left][grow,fill]", "") with IDomainPanelUI {

  val directoryTextField = new ChooseFileTextField(dataUI.directoryPath)
  contents += (new Label("List of files"), "gap para")
  contents += (directoryTextField, "wrap")

  def dataUI: FileDomainDataUI
}