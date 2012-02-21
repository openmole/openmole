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

package org.openmole.ide.misc.widget.example

import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel
import java.awt.Color
import java.awt.Dimension
import javax.swing.UIManager
import org.openmole.ide.misc.widget._
import scala.swing._

object PropertyPanelExample extends SimpleSwingApplication
{
  def top = new MainFrame {
    UIManager.setLookAndFeel(new NimbusLookAndFeel)
    title = "Link Label Demo"
    contents = new PropertyPanel(Color.green,"wrap"){
      contents +=new Label("first")
      contents += new TextField("TextField")
      contents += new ComboBox(List.empty)
      contents += new PluginPanel("wrap"){
        contents += new MainLinkLabel("second",new Action(""){def apply = println("My link")})
        contents += new Label("Tranparent panel")}
    }
    size = new Dimension(250,200)
  }
}