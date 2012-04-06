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
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import org.openmole.ide.core.model.panel.IPanelUI
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.ImageLinkLabel
import org.openmole.ide.misc.widget._
import scala.swing.Action
import scala.swing.Label
import scala.swing.event.UIElementResized
import scala.swing.TextField
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.misc.tools.image.Images._

object BasePanelUI {
  def imageIcon(proxy : IDataProxyUI) = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.fatImagePath)))
}

import BasePanelUI._

abstract class BasePanelUI(proxy: IDataProxyUI,
                           scene: IMoleScene,
                           mode : Value,
                           borderColor : Color = new Color(200,200,200)) extends MyPanel {
  peer.setLayout(new BorderLayout)
  val iconLabel = new Label{ icon = new ImageIcon(EMPTY)}
  val nameTextField = new TextField(15) {text = proxy.dataUI.name; tooltip = Help.tooltip("Name of the concept instance")}
  val createLabelLink = new MainLinkLabel("create",new Action("") { def apply = baseCreate})
  val mainLinksPanel = new PluginPanel("") {contents += createLabelLink}
  if (mode != CREATION) deleteLink
  border = BorderFactory.createEmptyBorder

  val mainPanel = new PluginPanel("wrap"){
    contents += new PluginPanel("wrap 2") {
      contents += iconLabel
      contents += new PluginPanel("wrap"){
        contents += new PluginPanel("wrap 2"){
          contents += nameTextField
          contents += new ImageLinkLabel(CLOSE,new Action("") { def apply = {
                mode match {
                  case EXTRA => scene.closeExtraPropertyPanel
                  case _ => scene.closePropertyPanel
                }}
          })
        }
        contents += mainLinksPanel
      }
    }
  }
  
  peer.add(mainPanel.peer,BorderLayout.CENTER)
  preferredSize.width = 300
  foreground = Color.white
  background = borderColor
  
  listenTo(this)
  reactions += {
    case x:UIElementResized => 
        scene.propertyWidget.revalidate
        scene.extraPropertyWidget.revalidate
        scene.refresh
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
    baseSave
    scene.refresh
  }
  
  def baseDelete: Unit = {
    delete
  }
  
  def baseSave : Unit = {
    save
    ConceptMenu.refreshItem(proxy)
    CheckData.checkMole(ScenesManager.currentSceneContainer.get.scene.manager)
  }
  
  def create: Unit
  
  def delete: Unit
  
  def save: Unit
  
  def panelUI: IPanelUI
}