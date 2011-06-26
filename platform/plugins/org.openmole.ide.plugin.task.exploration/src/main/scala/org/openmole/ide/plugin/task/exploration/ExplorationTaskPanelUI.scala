/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.task.exploration
import java.awt.Dimension
import scala.swing.ListView.Renderer
import scala.swing._
import swing.Swing._
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.properties.ITaskPanelUI

class ExplorationTaskPanelUI (pud: ExplorationTaskDataUI) extends BoxPanel(Orientation.Horizontal) with ITaskPanelUI {

  val pathFileLabel = new Label("Sampling :") {
    preferredSize = new Dimension(60,25)
    border = Swing.EmptyBorder(5,5,5,5)
  }
  
  val samplingCombo = new ComboBox(ElementFactories.dataSamplingProxys.toList) { renderer = Renderer(_.dataUI.name) }
  
  contents.append(pathFileLabel,samplingCombo)
  
  override def saveContent(name: String) = new ExplorationTaskDataUI(name,Some(samplingCombo.selection.item))
}
