package org.openmole.gui.client.core

import org.openmole.gui.ext.dataui.DataUI
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.Forms._
import scalatags.JsDom.short._

//import scalatags.JsDom.tags.{ div }

import scalatags.JsDom.attrs._
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

object GenericPanel {

  def apply(id: String, factories: Seq[FactoryUI], default: Option[DataUI] = None) = {

    factories.foreach { f ⇒ println("dis " + f.dataUI.getClass()) }
    val factorySelector = autoinput("factoryUI", "", factories)
    Rx {
      val content = Var(factorySelector.content())
      val panelUI = Var(content().dataUI.panelUI)
      val body = Var(panelUI().view)

      val header = Var(d(
        factorySelector.selector
      )
      )

      // modalHeader(headTag)

      val footer = Var(d( //  button("Close", btn_default, onclick := { () ⇒ panelUI().save("Yo") })
      )
      )

      // panelUI() = factorySelector.content().dataUI.panelUI
      // body() = modalBody((panelUI().view))
      println("modal dialoggg body " + body())
      println("modal dialoggg body render " + body().render)

      Rx {
        modalDialog(id,
          header,
          body,
          footer
        )
      }
    }
  }

}
