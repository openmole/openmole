package org.openmole.gui.client.core

import org.openmole.gui.client.service.ClientService
import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.{ Select, Forms ⇒ bs }
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

  val conceptTable = bs.table(
    thead,
    Rx {
      val dbUIs: Seq[DataBagUI] = filter().factories.head
      tbody({
        val elements = for (db ← dbUIs if filters(filter())(db)) yield {
          bs.tr(row)(
            bs.td(col_md_6)(a(db.name(), cursor := "pointer", onclick := { () ⇒
              setCurrent(db)
              editionState() = true
            })),
            bs.td(col_md_5)(bs.label(db.dataUI().dataType, label_primary)),
            bs.td(col_md_1)(bs.button(glyph(glyph_trash))(onclick := { () ⇒
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

  val inputFilter = bs.input(
    currentDataBagUI().map {
      _.name()
    }.getOrElse(""))(
      value := "",
      placeholder := "Filter",
      autofocus := "true"
    ).render

  inputFilter.oninput = (e: Event) ⇒ nameFilter() = inputFilter.value

  //New button
  val newGlyph = bs.button(glyph(glyph_plus))( /*`type` := "submit",*/ onclick := { () ⇒ add
  }).render

  def add = {
    val dbUI = new DataBagUI(Var(filter().factories.head.dataUI))
    dbUI.name() = inputFilter.value
    ClientService += dbUI
    setCurrent(dbUI)
    editionState() = true
  }

  val conceptFilter = Rx {
    nav("filterNav", nav_pills, navItem("allfilter", "All", () ⇒ {
      filter() = ALL
      factorySelector.contents() = ClientService.factories
    }),
      navItem("valfilter", "Val", () ⇒ {
        filter() = PROTOTYPES
        factorySelector.contents() = ClientService.prototypeFactories
      }),
      navItem("taskfilter", "Task", () ⇒ {
        filter() = TASKS
        factorySelector.contents() = ClientService.taskFactories
      }, true),
      navItem("envfilter", "Env", () ⇒ {
        println("not impl yet")
      })
    )
  }

  def setCurrent(dbUI: DataBagUI) = {
    currentDataBagUI() = Some(dbUI)
    factorySelector.content() = currentDataBagUI()
  }

  val saveHeaderButton = bs.button("Apply", btn_primary)( /*`type` := "submit",*/ onclick := { () ⇒
    save
  }).render

  val saveButton = bs.button("Close", btn_test)("data-dismiss".attr := "modal", onclick := { () ⇒
    save
  })

  val dialog = {
    modalDialog(uuid,
      bodyDialog(Rx {
        bs.div()(
          nav("navbar_form", navbar_form)(
            form(
              inputGroup(navbar_left)(
                inputFilter,
                inputGroupButton(newGlyph)
              ),
              if (editionState()) {
                bs.span(navbar_right)(
                  factorySelector.selector,
                  saveHeaderButton
                )
              }
              else bs.span(navbar_right)(conceptFilter), onsubmit := { () ⇒
                if (editionState()) save
                else if (rows() == 0) add
              }
            )
          ),
          if (editionState()) {
            inputFilter.value = currentDataBagUI().map {
              _.name()
            }.getOrElse((""))
            conceptPanel
          }
          else {
            inputFilter.value = ""
            bs.div()(conceptTable)
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
      bodyPanel(
        p.view)
    case _ ⇒ bs.div()(h1("Create a  first data !"))
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
      bs.nav("settingsNav", nav_pills,
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