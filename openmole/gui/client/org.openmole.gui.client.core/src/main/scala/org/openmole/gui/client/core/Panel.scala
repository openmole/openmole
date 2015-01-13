package org.openmole.gui.client.core

import org.openmole.gui.client.service.ClientService
import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.{ Select, ModalDialog, Forms }
import org.scalajs.dom.{ Event, HTMLDivElement, HTMLElement }

import scalatags.JsDom.all
import org.scalajs.jquery.jQuery

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
  def generic(uuid: String,
              factories: Seq[FactoryUI],
              dataBagUIs: Seq[DataBagUI],
              defaultProxyUI: Option[DataBagUI] = None,
              extraCategories: Seq[(String, HtmlTag)] = Seq()) =
    new GenericPanel(uuid, factories, dataBagUIs, defaultProxyUI, extraCategories).render()
}

/*
object PanelState {

  class PanelState(val name: String)

  case class Edition() extends PanelState("Edition")

  case class Read() extends PanelState("Read")

  case class Creation(val dataBagUI: DataBagUI) extends PanelState("Creation")

  def read = new Read

  def edition = new Edition

  def creation(db: DataBagUI) = new Creation(db)
}

import PanelState._*/

class GenericPanel(uuid: String,
                   factories: Seq[FactoryUI],
                   dataBagUIs: Seq[DataBagUI],
                   defaultDataBagUI: Option[DataBagUI] = None,
                   extraCategories: Seq[(String, HtmlTag)] = Seq()) {

  val jQid = "#" + uuid
  val editionState: Var[Boolean] = Var(false)

  val factorySelector = new Select[FactoryUI]("factories", Var(factories), defaultDataBagUI, btn_primary)

  val currentDataBagUI = Var(defaultDataBagUI)
  /*Rx {
     println("In CURRENT DATABAGUI RX")
     state() match {
       case c: Creation ⇒ Some(c.dataBagUI)
       case _           ⇒ ???
     }
   }*/

  val conceptTable = new ConceptTable(dataBagUIs)

  val inputFilter = Forms.input(
    currentDataBagUI().map {
      _.name()
    }.getOrElse(""))(
      placeholder := "Filter"
    ).render

  inputFilter.oninput = (e: Event) ⇒ conceptTable.nameFilter() = inputFilter.value

  val currentPanelUI = Rx(currentDataBagUI().map {
    _.dataUI().panelUI
  })

  //New button
  val newGlyph = Forms.button(glyph(glyph_plus))(onclick := { () ⇒
    println("-- NEWWWW")
    /*state() match {
      case e: Edition ⇒ println("edit")
      case _ ⇒
        println("NOT edit " + generateDataUI)
        generateDataUI.map { cdUI ⇒
          state() = creation(new DataBagUI(Var(cdUI)))
        }
    }*/
  })

  //Trash dataUI
  val trashDataUIGlyph = Forms.button(glyph(glyph_trash))(onclick := { () ⇒
    println("trash dataui clicked")
    currentDataBagUI().map {
      ClientService -=
    }
  })

  val saveHeaderButton = Forms.button("OK", btn_primary)(`type` := "submit", onclick := { () ⇒
    save
  })

  val cancelHeaderButton = Forms.button("Cancel")(onclick := { () ⇒
    // state() = read
  })

  val saveButton = Forms.button("Close", btn_primary)(dataWith("dismiss") := "modal", onclick := { () ⇒
    save
  })

  val panelFooter = h2(saveButton)

  /*val mdHeader = {
    form(
      Rx {
        println("In INPUT RX")
        Forms.formLine(
          // Rx {
          Forms.formGroup(
            state() match {
              case x @ (_: Edition | _: Creation) ⇒ dataBagNameInput
              case _                              ⇒ dataBagSelectorRender
            },
            //  if (dataBagUIEdition() || dataBagUICreation().isDefined) dataBagNameInput
            // else dataBagSelectorRender,
            editProxyUIGlyph
          ) //  }
          , {
            // factorySelector.visible() = dataBagUIEdition() || currentDataBagUI().isDefined
            factorySelectorRender
          },
          Forms.formGroup(

            Forms.buttonGroup()(
              newGlyph,
              trashDataUIGlyph
            )
          ),
          // Rx {
          state() match {
            case x @ (_: Edition | _: Creation) ⇒
              // case x if classOf[Edition].isAssignableFrom(x) || Creation ⇒
              val bg = Forms.buttonGroup(btn_group_small)(saveHeaderButton, cancelHeaderButton)
              val name = currentName
              dataBagNameInput.value = name
              dataBagNameInput.focus
              dataBagNameInput.selectionStart = name.size
              bg
            case _ ⇒ div()
          }
        )
      }
    )
  }*/

  val mdHeader = form(inputFilter)

  val mdBody = Rx {
    println("EDITION STATE " + editionState())
    if (editionState()) div()
    else div(conceptTable.view)
  }

  /*bodyPanel({
  currentPanelUI() match {
    case Some(p: PanelUI) ⇒
      println("change BODY PANEL " + p.view.toString())
      bodyPanel(p.view)
    case _ ⇒ div(h1("Create a  first data !"))
  }
}
)*/

  val mD /*: ModalDialog*/ = {
    println("IN MD RX")
    modalDialog(uuid,
      mdHeader,
      mdBody(),
      panelFooter)
  }

  def save: Unit = {
    /*  currentPanelUI().map { cpUI ⇒
      state() match {
        case c: Creation ⇒ ClientService += c.dataBagUI
        case _           ⇒
      }
      currentDataBagUI().map {
        db ⇒
          ClientService.name(db, inputFilter.value)
      }
      cpUI.save

      println("TASKS " + ClientService.taskDataBagUIs)
    }
    state() = read*/
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

  def generateDataUI: Option[DataUI] = factorySelector.content().map {
    _.dataUI
  }

  /*Obs(factorySelector.content, skipInitial = true) {
    println("factoriySelector OBS")
    generateDataUI.map { d ⇒
      currentDataBagUI().map {
        _.dataUI() = d
      }
    }
    println("DATAUI updated")
    currentPanelUI().map { cpUI ⇒
      mD.body() = mdBody
    }
  }*/

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