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

import javax.swing.BorderFactory
import java.awt.Color
import java.awt.Dimension
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import org.openmole.ide.misc.widget._
import scala.swing._
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget._
import org.netbeans.api.visual.graph.GraphScene
import scala.swing.event.UIElementResized

object PropertyPanelExample extends SimpleSwingApplication {
  def top = new MainFrame {
    title = "Link Label Demo"

    val scene = new MyScene
    val comboContent = List("un", "deux")
    val pp: Panel = new PluginPanel("wrap") {
      contents += new MultiCombo("",
        comboContent,
        List(new ComboPanel(comboContent, new ComboData(Some("deux"))),
          new ComboPanel(comboContent, new ComboData(Some("deux"))))).panel
      preferredSize = new Dimension(200, 200)

      listenTo(this)
      reactions += {
        case x: UIElementResized ⇒
          scene.compow.revalidate
          scene.compow.repaint
        //          
      }
    }

    //    contents = new PropertyPanel(Color.green,"wrap"){
    //      contents +=new Label("first")
    //      contents += new TextField("TextField")
    //      contents += new ComboBox(List.empty)
    //      contents += new PluginPanel("wrap"){
    //        contents += new MainLinkLabel("second",new Action(""){def apply = println("My link")})
    //        contents += new Label("Tranparent panel")}
    //        
    //        

    peer.setContentPane(scene.createView)
    scene.updatePanel(pp)
    minimumSize = new Dimension(400, 400)
  }

  class MyScene extends GraphScene.StringGraph {

    val panel = new PluginPanel("")
    val layer = new LayerWidget(this)
    addChild(layer)
    var compow = new ComponentWidget(this, panel.peer) {
      setBorder(BorderFactory.createLineBorder(Color.red, 2))
      setOpaque(true)
      setPreferredLocation(new Point(10, 10))
    }

    layer.addChild(compow)

    def updatePanel(p: Panel) = {
      panel.contents += p
      compow.setPreferredLocation(new Point(10, 10))
      repaint
      revalidate
    }

    def attachEdgeSourceAnchor(edge: String, oldSourceNode: String, sourceNode: String) = {}
    def attachEdgeTargetAnchor(edge: String, oldTargetNode: String, targetNode: String) = {}
    def attachNodeWidget(n: String) = new Widget(this)
    def attachEdgeWidget(n: String) = new Widget(this)
  }
}