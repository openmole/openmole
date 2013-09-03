package org.openmole.ide.plugin.task.stat

import scala.swing.{ Label, TextField }
import org.openmole.ide.misc.widget.PluginPanel

/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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

class ConfidenceIntervalTaskPanelUI(dataUI: ConfidenceIntervalTaskDataUI) extends BasicStatPanelUI("to interval", dataUI) {

  val levelTextField = new TextField(dataUI.level.toString, 10)
  panelSettings.contents.insert(0, new PluginPanel("wrap") {
    contents += new Label("Level")
    contents += levelTextField
  })

  def saveContent(name: String) = new ConfidenceIntervalTaskDataUI(name,
    multiPrototypeCombo.content.map { c ⇒ (c.comboValue1.get, c.comboValue2.get) },
    levelTextField.text.toDouble)
}