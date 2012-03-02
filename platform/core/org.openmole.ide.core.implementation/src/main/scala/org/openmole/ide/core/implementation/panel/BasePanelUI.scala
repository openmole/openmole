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

package org.openmole.ide.core.implementation.panel

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import org.openmole.ide.core.model.panel.IPanelUI
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.misc.widget._
import scala.swing.Action
import scala.swing.Label
import scala.swing.Panel
import scala.swing.event.UIElementResized
import scala.swing.TextField

abstract class BasePanelUI(proxy: IDataProxyUI,
                           scene: IMoleScene,
                           mode : Value,
                           borderColor : Color = new Color(200,200,200)) extends Panel {
  peer.setLayout(new BorderLayout)
  val iconLabel = new Label{ icon = new ImageIcon(ImageTool.loadImage("img/empty.png",50,50))}
  val nameTextField = new TextField(15) {text = proxy.dataUI.name; tooltip = Help.tooltip("Name of the concept instance")}
  val createLabelLink = new MainLinkLabel("create",new Action("") { def apply = baseCreate})
  val mainLinksPanel = new PluginPanel("") {contents += createLabelLink}
  if (mode == EDIT) deleteLink
  border = BorderFactory.createEmptyBorder

  val mainPanel = new PropertyPanel(borderColor,"wrap"){
    contents += new PluginPanel("wrap 2") {
      contents += iconLabel
      contents += new PluginPanel("wrap"){
        contents += new PluginPanel("wrap 2"){
          contents += nameTextField
          contents += new MainLinkLabel("",
                                        new Action("") { def apply = BasePanelUI.this.hide }) {
            icon = new ImageIcon(ImageTool.loadImage("img/close.png",20,20))}
        }
        contents += mainLinksPanel
      }
    }
  }
  peer.add(mainPanel.peer,BorderLayout.CENTER)
  preferredSize.width = 300
  foreground = Color.white
  
  mainPanel.contents.foreach { c =>
    listenTo(c)
    reactions += {
      case x:UIElementResized =>
        this.preferredSize =new Dimension(mainPanel.size.width + 30,mainPanel.size.height + 30)
        scene.refresh
    }
  }
  
  def hide = {
    baseSave
    visible = false
    scene.refresh
  }
  
  def deleteLink = {
    createLabelLink.link("delete")
    createLabelLink.action = new Action("") { def apply = baseDelete}
  }
  
  def baseCreate : Unit = {
    create  
    deleteLink
    scene.refresh
  }
  
  def baseDelete: Unit = {
    delete
    scene.removePropertyPanel
  }
  
  def baseSave : Unit = {
    save
    ConceptMenu.refreshItem(proxy)
  }
  
  def create: Unit
  
  def delete: Unit
  
  def save: Unit
  
  def panelUI: IPanelUI
}