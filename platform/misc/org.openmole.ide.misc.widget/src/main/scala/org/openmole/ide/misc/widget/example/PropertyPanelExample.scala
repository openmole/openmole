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
import javax.swing.JScrollPane
import javax.swing.UIManager
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget._
import scala.swing._
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.Scene

object PropertyPanelExample extends SimpleSwingApplication
{
  def top = new MainFrame {
    UIManager.setLookAndFeel(new NimbusLookAndFeel)
    title = "Link Label Demo"
    
    val scene = new Scene
    
    
    
    contents = new PropertyPanel(Color.green,"wrap"){
      contents +=new Label("first")
      contents += new TextField("TextField")
      contents += new ComboBox(List.empty)
      contents += new PluginPanel("wrap"){
        contents += new MainLinkLabel("second",new Action(""){def apply = println("My link")})
        contents += new Label("Tranparent panel")}
      
     // val sp = new ScrollPane(scene.createView)
     // contents += sp
      
      val pp = new PluginPanel("wrap"){
        contents += new MultiCombo("",List("un","deux"),List("un","un")).panel}
      
      val compow = new ComponentWidget(scene,pp.peer)
      scene.addChild(compow)
      compow.setPreferredLocation(new Point(10,10))
      listenTo(pp)
      reactions += {
        case x : PluginPanelResizedEvent => 
          println("XXXXXXXXXXXXXXXX property resized :: " + size.height)
          revalidate
          repaint
          
        case x : Any => println ("yYYYYYYYYYYYYY " + size.height)
          revalidate
          repaint
      }
  
    }
    minimumSize = new Dimension(100,100)
  }
}