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
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dataproxy.{ UpdatedProxyEvent, Proxys }
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent
import scala.collection.JavaConversions._
import scala.swing.Component
import scala.swing.event.FocusGained
import javax.imageio.ImageIO
import BasePanel.IconChanged

object PrototypePanel {
  def deletePrototype(proxy: IPrototypeDataProxyUI,
                      scene: IMoleScene,
                      index: Int): Boolean = {
    def erase = {
      scene.closePropertyPanel(index)
      Proxys -= proxy
      // Proxys.prototypes.filter { p ⇒ p.dataUI.name == proxy.dataUI.name && p.generated }.foreach { p ⇒ Proxys -= p }
      ConceptMenu.removeItem(proxy)
      true
    }

    //remove in Tasks
    val capsulesWithProtos: List[ICapsuleUI] = ScenesManager.moleScenes.flatMap {
      _.manager.capsules.values.flatMap { c ⇒
        c.dataUI.task match {
          case Some(x: ITaskDataProxyUI) ⇒ if (x.dataUI.filterPrototypeOccurencies(proxy).isEmpty) None else Some(c)
          case _ ⇒ None
        }
      }
    }.toList

    //  capsulesWithProtos match {
    // case Nil ⇒ erase
    // case _ ⇒
    if (DialogFactory.deleteProxyConfirmation(proxy)) {
      Proxys.hooks.foreach { h ⇒
        h.dataUI.removePrototypeOccurencies(proxy)
        h.dataUI = h.dataUI.cloneWithoutPrototype(proxy)
      }
      Proxys.sources.foreach { s ⇒
        s.dataUI.removePrototypeOccurencies(proxy)
        s.dataUI = s.dataUI.cloneWithoutPrototype(proxy)
      }
      erase
      capsulesWithProtos.foreach { _.dataUI.task.get.dataUI.removePrototypeOccurencies(proxy) }
      List(ScenesManager.currentScene).flatten.foreach {
        _.manager.connectors.values.toList.foreach {
          dc ⇒ dc.filteredPrototypes = dc.filteredPrototypes.filterNot { _ == proxy }
        }
      }
      ScenesManager.invalidateMoles
      true
    } else false
    //  }
  }
}

import PrototypePanel._
class PrototypePanel[T](proxy: IPrototypeDataProxyUI,
                        scene: IMoleScene,
                        val index: Int) extends BasePanel(Some(proxy), scene) {
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.fatImagePath)))
  val panelUI = proxy.dataUI.buildPanelUI
  def created = Proxys.contains(proxy)

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.peer
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  listenTo(panelUI)
  listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
    case IconChanged(_, iconPath) ⇒
      iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(iconPath)))
  }

  def create = {
    Proxys += proxy
    ConceptMenu.prototypeMenu.popup.contents += ConceptMenu.addItem(nameTextField.text,
      proxy)
  }

  def delete = deletePrototype(proxy, scene, index)

  def save = {
    proxy.dataUI = panelUI.saveContent(nameTextField.text)
    publish(new UpdatedProxyEvent(proxy, this))
  }

}