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

package org.openmole.ide.plugin.environment.glite

import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import scala.swing.BoxPanel
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.Separator
import scala.swing.Swing
import scala.swing.TextField
import java.awt.Dimension

class GliteEnvironmentPanelUI(pud: GliteEnvironmentDataUI) extends BoxPanel(Orientation.Vertical) with IEnvironmentPanelUI{
  maximumSize = new Dimension(300,500)
  border = Swing.EmptyBorder(10, 10, 10, 10)
  yLayoutAlignment = 0
  xLayoutAlignment = 0
  
  val voTextField = new TextField{size.height = 30;border = Swing.EmptyBorder(5,5,5,5)}
  val vomsTextField = new TextField{size.height = 30;border = Swing.EmptyBorder(5,5,5,5)}
  val bdiiTextField = new TextField{size.height = 30;border = Swing.EmptyBorder(5,5,5,5)}
  
  contents+= new BoxPanel(Orientation.Horizontal){contents.append(new Label("VO: "),voTextField)}
  contents+= new BoxPanel(Orientation.Horizontal){contents.append(new Label("VOMS: "),vomsTextField)}
  contents+= new BoxPanel(Orientation.Horizontal){contents.append(new Label("BDIi: "),bdiiTextField)}
  contents+= new Separator
  
  voTextField.text = pud.vo
  vomsTextField.text = pud.voms
  bdiiTextField.text = pud.bdii
  
  override def saveContent(name: String) = new GliteEnvironmentDataUI(name,
                                                                       voTextField.text,
                                                                       vomsTextField.text,
                                                                       bdiiTextField.text)
}
