package org.openmole.gui.client.core

import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.ModalDialog

import scalatags.JsDom.all

import scalatags.JsDom.all._

import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.client.service.ClientService._
import org.openmole.gui.misc.js.JsRxTags._
import rx._
/*
 * Copyright (C) 12/11/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object Panel {
  def generic(uuid: String, factories: Seq[FactoryUI], default: Option[DataUI] = None) = {
    val defaultDataUI: DataUI = default.getOrElse(factories.head.dataUI)
    val defaultFactoryUI: Option[FactoryUI] = defaultDataUI

    factories.foreach { f ⇒ println("DIIS " + f.dataUI.getClass()) }
    val factorySelector = autoinput("factoryUI", factories, default = defaultFactoryUI)

    val panelHeader = div(factorySelector.selector)

    val panelFooter = div( //  button("Close", btn_default, onclick := { () ⇒ panelUI().save("Yo") })
    )

    val mD = modalDialog(uuid,
      panelHeader,
      Var(factorySelector.content().dataUI.panelUI.view),
      panelFooter)

    Obs(factorySelector.content) {
      mD.body() = factorySelector.content().dataUI.panelUI.view
    }

    mD.shell

  }
}

/*
class PanelWithIO[T <: IODataUI](id: String, factories: Seq[IOFactoryUI], default: Option[T] = None) extends GenericPanel(id, factories, default) {
  override val _default: IODataUI = default.getOrElse(factories.head.dataUI)
  // type DEF_DATAUI = IODataUI
  val ioPanel = new IOPanel(_default)

  override val body =
    nav(nav_pills,
      navItem("Settings", /*panelUI().view*/ d()),
      navItem("I/O", ioPanel.render))

}*/ 