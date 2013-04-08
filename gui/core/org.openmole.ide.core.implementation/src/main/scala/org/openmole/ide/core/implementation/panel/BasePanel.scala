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
import org.openmole.ide.core.model.dataproxy.{ IPrototypeDataProxyUI, ISamplingCompositionDataProxyUI, IDataProxyUI }
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget._
import swing._
import event.UIElementResized
import scala.swing.event.ActionEvent
import scala.swing.event.UIElementResized
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.tools.image.Images._
import scala.Some
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, UpdatedProxyEvent }

object BasePanel {
  case class IconChanged(s: Component, imagePath: String) extends ActionEvent(s)
}

abstract class BasePanel(proxy: Option[IDataProxyUI],
                         scene: IMoleScene) extends MyPanel with IBasePanel {
  scene.manager.refreshCache
  opaque = true
  var tabbedLock = false

  peer.setLayout(new BorderLayout)
  val iconLabel = new Label { icon = new ImageIcon(EMPTY) }

  val nameTextField = new TextField(15)

  proxy match {
    case Some(p: IDataProxyUI) ⇒ nameTextField.text = p.dataUI.name
    case _ ⇒
  }

  val createLabelLink = new MainLinkLabel("create", new Action("") { def apply = baseCreate })
  val mainLinksPanel = new PluginPanel("")
  if (created) deleteLink
  border = BorderFactory.createEmptyBorder

  val mainPanel = new PluginPanel("wrap", "", "") {
    contents += new PluginPanel("", "[left]", "[top]") {
      contents += new ImageLinkLabel(CLOSE, new Action("") {
        def apply = {
          scene.closePropertyPanel(index)
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

  val tabbedPane = new TabbedPane {
    preferredSize = new Dimension(200, 100)
    tabPlacement = Alignment.Left
    opaque = true
    background = new Color(77, 77, 77)
  }

  preferredSize.width = 300
  foreground = Color.white

  listenTo(this)
  reactions += {
    case x: UIElementResized ⇒
      scene.propertyWidget.foreach { _.revalidate }
      scene.refresh
  }

  def setTab(proxy: IDataProxyUI) = tabbedPane.selection.index = proxy match {
    case p: ISamplingCompositionDataProxyUI ⇒ 0
    case p: IPrototypeDataProxyUI ⇒ 1
    case _ ⇒ 1
  }

  def refreshPanel = {
    tabbedPane.pages.clear
    panelUI.components.foreach { c ⇒
      tabbedPane.pages += new TabbedPane.Page(c._1, c._2)
    }
  }

  def deleteLink = {
    createLabelLink.link("delete")
    createLabelLink.action = new Action("") { def apply = baseDelete }
  }

  def baseCreate: Unit = {
    create
    publishCreation
    deleteLink
    baseSave
    scene.manager.invalidateCache
    scene.refresh
  }

  def baseDelete: Boolean = {
    val b = delete
    scene.manager.invalidateCache
    b
  }

  def publishCreation = proxy match {
    case Some(x: IDataProxyUI) ⇒ publish(new UpdatedProxyEvent(x, this))
    case _ ⇒
  }

  def baseSave: Unit = {
    save
    scene.manager.invalidateCache
    proxy match {
      case Some(p: IDataProxyUI) ⇒ ConceptMenu.refreshItem(p)
      case _ ⇒
    }
    ScenesManager.currentSceneContainer match {
      case Some(x: ISceneContainer) ⇒ CheckData.checkMole(x.scene)
      case None ⇒
    }
  }
}