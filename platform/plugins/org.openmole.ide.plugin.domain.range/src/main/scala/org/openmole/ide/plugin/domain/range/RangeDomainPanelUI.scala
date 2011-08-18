/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.domain.range

import java.text.Format
import scala.swing.BoxPanel
import scala.swing.FormattedTextField
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.Swing

class RangeDomainPanelUI(format: Format) extends BoxPanel(Orientation.Vertical){
  border = Swing.EmptyBorder(10, 10, 10, 10)
  yLayoutAlignment = 0
  xLayoutAlignment = 0
  
  val minField = new FormattedTextField(format){size.height = 30;border = Swing.EmptyBorder(5,5,5,5)}
  val maxField = new FormattedTextField(format){size.height = 30;border = Swing.EmptyBorder(5,5,5,5)}
  val stepField = new FormattedTextField(format){size.height = 30;border = Swing.EmptyBorder(5,5,5,5)}
  
  contents+= new BoxPanel(Orientation.Horizontal){contents.append(new Label("Min: "),minField)}
  contents+= new BoxPanel(Orientation.Horizontal){contents.append(new Label("Max: "),maxField)}
  contents+= new BoxPanel(Orientation.Horizontal){contents.append(new Label("Step: "),stepField)}
}
