package org.openmole.gui.client.core

import org.openmole.gui.client.service.ClientService
import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.{ Select, Forms }
import org.scalajs.dom.Event

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

  object ConceptFilter extends Enumeration {

    case class ConceptState(name: String, factories: Seq[FactoryUI]) extends Val(name)

    val ALL = ConceptState("All", ClientService.factories)
    val TASKS = ConceptState("Tasks", ClientService.taskFactories)
    val PROTOTYPES = ConceptState("Prototypes", ClientService.prototypeFactories)
  }

  import ConceptFilter._

  def generic(uuid: String,
              defaultDataBagUI: Either[DataBagUI, ConceptState] = Right(ALL),
              extraCategories: Seq[(String, HtmlTag)] = Seq()) =
    new GenericPanel(uuid, defaultDataBagUI, extraCategories).dialog
}

import Panel.ConceptFilter._

class GenericPanel(uuid: String,
                   defaultDataBagUI: Either[DataBagUI, ConceptState] = Right(ALL),
                   extraCategories: Seq[(String, HtmlTag)] = Seq()) {

  val jQid = "#" + uuid
  val editionState: Var[Boolean] = Var(false)
  val filter: Var[ConceptState] = Var(defaultDataBagUI.right.toOption.getOrElse(ALL))
  var nameFilter = Var("")
  val rows = Var(0)
  val currentDataBagUI = Var(defaultDataBagUI.left.toOption)
  val currentPanelUI = Rx(currentDataBagUI().map {
    _.dataUI().panelUI
  })
  val factorySelector = new Select[FactoryUI]("factories", Var(filter().factories), currentDataBagUI(), btn_primary)

  Obs(factorySelector.content) {
    currentDataBagUI().map {
      _.dataUI() = factorySelector.content().map {
        _.dataUI
      }.get
    }
  }

  def contains(db: DataBagUI) = db.name().contains(nameFilter())

  private val filters = Map[ConceptState, DataBagUI ⇒ Boolean](
    (ALL, contains),
    (TASKS, db ⇒ isTaskUI(db) && contains(db)),
    (PROTOTYPES, db ⇒ isPrototypeUI(db) && contains(db))
  )

  val conceptTable = Forms.table(
    thead,
    Rx {
      val dbUIs: Seq[DataBagUI] = filter().factories.head
      tbody({
        val elements = for (db ← dbUIs if filters(filter())(db)) yield {
          tr(`class` := "row",
            td(a(db.name(), cursor := "pointer", onclick := { () ⇒
              setCurrent(db)
              editionState() = true
            }))(`class` := "col-md-5"),
            td(Forms.label(db.dataUI().dataType, label_primary + "col-md-5")),
            td(Forms.button(glyph(glyph_trash), "col-md-2")(onclick := { () ⇒
              ClientService -= db
            }))
          )
        }
        rows() = elements.size
        elements
      }
      )
    }
  ).render

  val inputFilter = Forms.input(
    currentDataBagUI().map {
      _.name()
    }.getOrElse(""))(
      value := "",
      placeholder := "Filter",
      autofocus := "true"
    ).render

  inputFilter.oninput = (e: Event) ⇒ nameFilter() = inputFilter.value

  //New button
  val newGlyph = Forms.button(glyph(glyph_plus))( /*`type` := "submit",*/ onclick := { () ⇒ add
  }).render

  def add = {
    val dbUI = new DataBagUI(Var(filter().factories.head.dataUI))
    dbUI.name() = inputFilter.value
    ClientService += dbUI
    setCurrent(dbUI)
    editionState() = true
  }

  val conceptFilter = Rx {
    nav(nav_pills,
      navItem("valfilter", "Val", () ⇒ {
        filter() = PROTOTYPES
        factorySelector.contents() = ClientService.prototypeFactories
      }),
      navItem("taskfilter", "Task", () ⇒ {
        filter() = TASKS
        factorySelector.contents() = ClientService.taskFactories
      }),
      navItem("envfilter", "Environments", () ⇒ {
        println("not impl yet")
      })
    )
  }

  /* def setActive(id: String) = {
    println("SET active " + id)
    jQuery(".active").removeClass("active")
    jQuery("#" + id).addClass("active")
  }*/

  def setCurrent(dbUI: DataBagUI) = {
    currentDataBagUI() = Some(dbUI)
    factorySelector.content() = currentDataBagUI()
  }

  val saveHeaderButton = Forms.button("Apply", btn_primary)( /*`type` := "submit",*/ onclick := { () ⇒
    save
  }).render

  val saveButton = Forms.button("Close", btn_primary)("data-dismiss".attr := "modal", onclick := { () ⇒
    save
  })

  val dialog = {
    modalDialog(uuid,
      bodyDialog(Rx {
        div(
          form(
            formLine(
              inputGroup(
                inputFilter,
                inputGroupButton(newGlyph)
              ),
              if (editionState()) {
                span(
                  factorySelector.selector,
                  saveHeaderButton
                )
              }
              else conceptFilter
            ), onsubmit := { () ⇒
              if (editionState()) save
              else if (rows() == 0) add
            }
          ),
          if (editionState()) {
            inputFilter.value = currentDataBagUI().map {
              _.name()
            }.getOrElse((""))
            conceptPanel
          }
          else {
            inputFilter.value = ""
            div(conceptTable)
          }
        )
      }
      ),
      footerDialog(
        h2(saveButton)
      )
    )
  }.render

  def conceptPanel = currentPanelUI() match {
    case Some(p: PanelUI) ⇒
      bodyPanel(p.view)
    case _ ⇒ div(h1("Create a  first data !"))
  }

  def save = {
    currentDataBagUI().map {
      db ⇒
        ClientService.setName(db, inputFilter.value)
    }

    currentPanelUI().map { cpUI ⇒
      cpUI.save
    }

    editionState() = false
    nameFilter() = ""
  }

  def bodyPanel(view: HtmlTag) = extraCategories.size match {
    case 0 ⇒ view
    case _ ⇒
      Forms.nav(nav_pills,
        (for (c ← ("Settings", view) +: extraCategories) yield {
          navItem(c._1, c._1)
        }): _*
      )
  }

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