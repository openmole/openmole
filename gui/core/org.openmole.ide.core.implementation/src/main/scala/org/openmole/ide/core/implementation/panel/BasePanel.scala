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
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import org.openmole.ide.core.model.panel.{ IBasePanel, IPanelUI }
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget._
import scala.swing.Action
import scala.swing.Component
import scala.swing.Label
import scala.swing.event.ActionEvent
import scala.swing.event.UIElementResized
import scala.swing.Publisher
import scala.swing.TextField
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.tools.image.Images._

object BasePanel {
  case class IconChanged(s: Component, imagePath: String) extends ActionEvent(s)
}

abstract class BasePanel(proxy: Option[IDataProxyUI],
                         scene: IMoleScene,
                         val mode: Value) extends MyPanel with IBasePanel {

  opaque = true
  peer.setLayout(new BorderLayout)
  val iconLabel = new Label { icon = new ImageIcon(EMPTY) }

  val nameTextField = new TextField(15)

  proxy match {
    case Some(p: IDataProxyUI) ⇒ nameTextField.text = p.dataUI.name
    case _ ⇒
  }

  val createLabelLink = new MainLinkLabel("create", new Action("") { def apply = baseCreate })
  val mainLinksPanel = new PluginPanel("")
  if (mode != CREATION && mode != EXTRA_CREATION) deleteLink
  border = BorderFactory.createEmptyBorder

  val mainPanel = new PluginPanel("wrap", "", "") {
    contents += new PluginPanel("", "[left]", "[top]") {
      contents += new ImageLinkLabel(CLOSE, new Action("") {
        def apply = {
          mode match {
            case EXTRA | EXTRA_CREATION ⇒ scene.closeExtraPropertyPanel
            case _ ⇒ scene.closePropertyPanel
          }
        }
      })
    }

    proxy match {
      case Some(p: IDataProxyUI) ⇒
        add(new PluginPanel("wrap 3", "[left]", "[center]") {
          contents += iconLabel
          contents += nameTextField
          contents += createLabelLink
        }, "gapbottom 10")
        listenTo(nameTextField)
      case _ ⇒
    }
  }

  preferredSize.width = 300
  foreground = Color.white

  listenTo(this)
  reactions += {
    case x: UIElementResized ⇒
      scene.propertyWidget.revalidate
      scene.extraPropertyWidget.revalidate
      scene.refresh
  }

  var created = if (mode == CREATION || mode == EXTRA_CREATION) false else true

  def hide = {
    baseSave
    visible = false
    scene.refresh
  }

  def deleteLink = {
    createLabelLink.link("delete")
    createLabelLink.action = new Action("") { def apply = baseDelete }
  }

  def baseCreate: Unit = {
    create
    created = true
    deleteLink
    baseSave
    scene.refresh
  }

  def baseDelete: Boolean = {
    delete
  }

  def baseSave: Unit = {
    save
    proxy match {
      case Some(p: IDataProxyUI) ⇒ ConceptMenu.refreshItem(p)
      case _ ⇒
    }
    ScenesManager.currentSceneContainer match {
      case Some(x: ISceneContainer) ⇒ CheckData.checkMole(x.scene)
      case None ⇒
    }
  }

  def create: Unit

  def delete: Boolean

  def save: Unit

  def panelUI: IPanelUI
}