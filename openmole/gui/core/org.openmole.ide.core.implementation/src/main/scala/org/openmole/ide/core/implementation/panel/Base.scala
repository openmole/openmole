package org.openmole.ide.core.implementation.panel

import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.implementation.dataproxy.{ ProxyDeletedEvent, ProxyCreatedEvent, Proxies }
import org.openmole.misc.eventdispatcher._
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.event.{ UIElementResized, MouseClicked }
import scala.swing.TabbedPane

/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

trait Base extends Created with SavePanel {

  val scene: MoleScene

  val index: Int

  def update: Unit = {}

  def createSettings: Unit

  def created: Boolean

  val basePanel = new PluginPanel("wrap") {
    listenTo(this, mouse.clicks)
    reactions += {
      case e: MouseClicked ⇒
      case x: UIElementResized ⇒
        scene.propertyWidget.foreach { _.revalidate }
        scene.graphScene.validate
        scene.graphScene.repaint
    }
  }

  val createListener = new EventListener[Proxies] {
    def triggered(obj: Proxies, ev: Event[Proxies]) {
      scene.updatePanels
    }
  }

  val deleteListener = new EventListener[Proxies] {
    def triggered(obj: Proxies, ev: Event[Proxies]) {
      scene.updatePanels
    }
  }

  EventDispatcher.listen(Proxies.instance, createListener, classOf[ProxyCreatedEvent])
  EventDispatcher.listen(Proxies.instance, deleteListener, classOf[ProxyDeletedEvent])
}
