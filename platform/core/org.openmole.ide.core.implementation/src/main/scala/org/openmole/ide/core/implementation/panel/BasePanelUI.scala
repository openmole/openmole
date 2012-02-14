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

import javax.swing.ImageIcon
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget._
import scala.swing.Action
import scala.swing.Label
import scala.swing.ScrollPane
import scala.swing.TextField

abstract class BasePanelUI(proxy: IDataProxyUI,
                           scene: IMoleScene,
                           mode : Value) extends ScrollPane {

  val iconLabel = new Label{ icon = new ImageIcon(ImageTool.loadImage("img/empty.png",50,50))}
  val nameTextField = new TextField(15) {text = proxy.dataUI.name; tooltip = Help.tooltip("Name of the concept instance")}
  val labelLink = new LinkLabel("create",new Action("") { def apply = baseCreate})
  if (mode == EDIT) deleteLink
  
  val mainPanel = new MigPanel("wrap"){
    contents += new MigPanel("wrap 2") {
      contents += iconLabel
      contents += new MigPanel("wrap"){
        contents += nameTextField
        contents += labelLink
      }
    }
  }
  contents = mainPanel
  preferredSize.width = 200
  
  verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
  horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
  
  def deleteLink = {
    labelLink.link("delete")
    labelLink.action = new Action("") { def apply = baseDelete}
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
}