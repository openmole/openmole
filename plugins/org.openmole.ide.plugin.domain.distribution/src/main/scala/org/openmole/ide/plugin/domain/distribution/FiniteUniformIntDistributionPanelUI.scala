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

package org.openmole.ide.plugin.domain.distribution

import scala.swing._
import swing.Swing._
import swing.ListView._
import scala.swing.Table.ElementMode._
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.BorderPanel.Position._

class FiniteUniformIntDistributionPanelUI(pud: FiniteUniformIntDistributionDataUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IDomainPanelUI {
  val sizeField = new TextField(6)

  contents += (new Label("Size"), "gap para")
  contents += (sizeField, "wrap")

  sizeField.text = pud.size.toString

  override def saveContent(name: String) = {
    new FiniteUniformIntDistributionDataUI(
      name,
      sizeField.text.toInt)
  }
}
