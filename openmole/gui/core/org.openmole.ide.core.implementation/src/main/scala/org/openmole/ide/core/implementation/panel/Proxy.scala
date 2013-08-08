/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.ide.misc.widget.{ MainLinkLabel, PluginPanel }
import org.openmole.ide.core.implementation.dataproxy.{ DataProxyFactory, Proxies, DataProxyUI }
import scala.swing.{ Label, Action, TextField }
import ConceptMenu._
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.implementation.data._

trait IOProxy extends Proxy {
  type IODATAUI <: DataUI with ImageView with InputPrototype with OutputPrototype with ImplicitPrototype
  type DATAPROXY <: DataProxyUI with IOFacade
}

trait IOFacade {
  var dataUI: DATAUI
  type DATAUI <: DataUI with ImageView with InputPrototype with OutputPrototype with ImplicitPrototype
}

trait Proxy {

  type DATAPROXY <: DataProxyUI
  val proxy: DATAPROXY

  val nameTextField = new TextField(10)

  def proxyCreated = Proxies.instance.contains(proxy)

  private val createLabelLink = new MainLinkLabel("create",
    new Action("") {
      def apply = create
    }
  )

  private def deleteLink = {
    createLabelLink.link("delete")
    createLabelLink.action = new Action("") {
      def apply = deleteProxy
    }
  }

  class Composer extends PluginPanel("width 375::", "[left]", "[center]") {

    def addIcon(icon: Label) = contents += icon

    def addName = {
      nameTextField.text = proxy.dataUI.name
      contents += nameTextField
    }

    def addCreateLink = {
      contents += createLabelLink
      if (proxyCreated) deleteLink
    }

    def addTypeMenu(menu: PopupToolBarPresenter) =
      contents += menu
  }

  def create: Unit = {
    Proxies.instance += proxy
    +=(nameTextField.text, proxy)
    toDelete
    ScenesManager.invalidateSceneCaches
    ScenesManager.refreshScenes
  }

  def deleteProxy: Unit

  private def toDelete = {
    createLabelLink.link("delete")
    createLabelLink.action = new Action("") {
      def apply = deleteProxy
    }
  }

}