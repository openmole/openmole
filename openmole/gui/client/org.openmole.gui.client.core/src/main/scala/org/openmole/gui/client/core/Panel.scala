package org.openmole.gui.client.core

import org.openmole.gui.client.service.ClientService
import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.ModalDialog
import org.scalajs.dom.{ HTMLDivElement, HTMLElement }

import scalatags.JsDom.all
import org.scalajs.jquery.jQuery

import scalatags.JsDom.all._

import org.openmole.gui.misc.js.Forms
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
  def generic(uuid: String,
              factories: Seq[FactoryUI],
              dataUIs: Seq[DataUI],
              default: Option[DataUI] = None,
              extraCategories: Seq[(String, HtmlTag)] = Seq()) =
    new GenericPanel(uuid, factories, dataUIs, default, extraCategories).render()
}

class GenericPanel(uuid: String,
                   factories: Seq[FactoryUI],
                   dataUIs: Seq[DataUI],
                   default: Option[DataUI] = None,
                   extraCategories: Seq[(String, HtmlTag)] = Seq()) {

  val jQid = "#" + uuid
  val dataUI: Var[DataUI] = Var(default.getOrElse(factories.head.dataUI))
  // val factoryUI: Var[FactoryUI] = Var(dataUI().getOrElse(factories.head))
  val panelUI: Var[Option[PanelUI]] = Var(
    Some(dataUI().panelUI)
  )

  val dataUIEdition: Var[Boolean] = Var(false)

  //DataUI in Edition mode or not
  val dataSelector = autoselect(("dataUI"), dataUIs, default = Some(dataUI()))
  val dataSelectorRender = dataSelector.selector
  val dataInput = input(id := "dataUIInput", `type` := "text")(dataSelector.contentName).render

  //FactoryUI in Edition mode or not
  val factorySelector = autoselect("factoryUI", factories, default = Some(dataUI().getOrElse(factories.head)))
  val factorySelectorRender = factorySelector.selector

  def currentFactory = factorySelector.content().get

  //Edit dataUI
  val editDataUIGlyph = Forms.button(glyph(glyph_edit))(onclick := { () ⇒
    println("edit dataui clicked")
    editDataUI(true)
  })

  //New button
  val newGlyph = Forms.button(glyph(glyph_plus))(onclick := { () ⇒
    dataUI() = currentFactory.dataUI
    editDataUI(true)
  })

  //Trash dataUI
  val trashDataUIGlyph = Forms.button(glyph(glyph_trash))(onclick := { () ⇒
    println("trash dataui clicked")
    ClientService -= dataUI
  })

  def editDataUI(b: Boolean): Unit = {
    dataUIEdition() = b
    println("DATAUI " + dataUI().name())
    println("DATA INPUT  " + dataInput.value)
    dataInput.value = dataUI().name()
    println("dataInput " + dataInput.value)
  }

  val headerPanel = form(
    Forms.formLine(Rx {
      Forms.formGroup(
        if (dataUIEdition()) dataInput
        else dataSelectorRender,
        editDataUIGlyph
      )
    },
      factorySelectorRender,
      Forms.formGroup(
        Forms.buttonGroup(
          newGlyph,
          trashDataUIGlyph
        )
      )
    )
  )

  /*(onsubmit := { () ⇒
       println("input submitted")
       dataUIEdition() = false
     })*/

  val panelFooter =
    h2(
      Forms.button("Close", btn_primary)(dataWith("dismiss") := "modal", onclick := { () ⇒
        save
      })
    )

  val mD: ModalDialog = modalDialog(uuid,
    headerPanel,
    Var(bodyPanel(panelUI() match {
      case Some(p: PanelUI) ⇒ bodyPanel(p.view)
      case _                ⇒ div()
    })),
    panelFooter)

  def save = panelUI() match {
    case Some(p: PanelUI) ⇒
      println(dataUIEdition() + " crrent name " + currentName)
      ClientService += p.save(currentName)
      dataSelector.contents() = ClientService.taskDataUIs
      println("taskdataUIs :" + ClientService.taskDataUIs)
      dataUIEdition() = false
    case _ ⇒
  }

  private def currentName = dataUIEdition() match {
    case true ⇒ dataInput.value
    case _    ⇒ dataSelector.contentName
  }

  def bodyPanel(view: HtmlTag) = extraCategories.size match {
    case 0 ⇒ view
    case _ ⇒
      nav(nav_pills,
        (for (c ← ("Settings", view) +: extraCategories) yield {
          navItem(c._1)(c._2)
        }): _*
      )
  }

  Obs(dataSelector.content) {
    println("Data Obs")
    panelUI() = dataSelector.content() match {
      case Some(dataUI: DataUI) ⇒ Some(dataUI.panelUI)
      case _                    ⇒ None
    }
  }

  Obs(factorySelector.content) {
    println("Factory Obs")
    panelUI() = Some(currentFactory.dataUI.panelUI)
    mD.body() = bodyPanel(panelUI().get.view)
  }

  def render = mD.shell

}

/*
class PanelWithIO[T <: IODataUI](id: String, factories: Seq[IOFactoryUI], default: Option[T] = None) /*extends GenericPanel(id, factories, default) */ {
  val _default: IODataUI = default.getOrElse(factories.head.dataUI)
  // type DEF_DATAUI = IODataUI
  val panel = Panel.generic("taskPanelID", factories)
  val ioPanel = new IOPanel(_default)

  val body =
    nav(nav_pills,
      navItem("Settings", panel.render) /*,
      navItem("I/O", ioPanel.render)*/ )

}*/ 