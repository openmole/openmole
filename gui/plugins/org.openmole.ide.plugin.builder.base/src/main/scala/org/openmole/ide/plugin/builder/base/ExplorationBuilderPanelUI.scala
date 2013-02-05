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

package org.openmole.ide.plugin.builder.base

import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.builder._
import org.openmole.core.implementation.puzzle.Puzzle
import swing.{ MyComboBox, Label }
import org.openmole.plugin.builder.base._
import org.openmole.ide.core.model.workflow.IMoleSceneManager

class ExplorationBuilderPanelUI(puzzle: Puzzle, manager: IMoleSceneManager) extends BuilderPanel {

  val samplingComboBox = new MyComboBox(Proxys.samplings.toSeq)
  contents += new Label("Sampling")
  contents += samplingComboBox

  def build = exploration(nameTextField.text, puzzle, Proxys.getOrGenerateSamplingComposition(samplingComboBox.selection.item).dataUI.coreObject)(manager.dataUI.pluginSet)
}
