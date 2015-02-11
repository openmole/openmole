package org.openmole.gui.client.core

import org.openmole.gui.client
import org.openmole.gui.client.service.ClientService
import org.openmole.gui.client.service.dataui._
import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.{ Forms ⇒ bs, InputFilter, Select }
import org.scalajs.dom.Event

import scala.sys.Prop.DoubleProp
import scalatags.JsDom.all
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.ext.data.ProtoTYPE.DOUBLE
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
    val PROTOTYPES = ConceptState("Prototypes",
      PrototypeFactoryUI.doubleFactory +: ClientService.prototypeFactories filterNot (_.dataUI.dataType == DOUBLE)

    )
  }

  import ConceptFilter._

  def generic(uuid: String,
              defaultDataBagUI: Either[DataBagUI, ConceptState] = Right(TASKS)) =
    new GenericPanel(uuid, defaultDataBagUI).dialog
}

import Panel.ConceptFilter._

class GenericPanel(uuid: String,
                   defaultDataBagUI: Either[DataBagUI, ConceptState] = Right(TASKS)) {

  val editionState: Var[Boolean] = Var(false)
  val filter: Var[ConceptState] = Var(defaultDataBagUI.right.toOption.getOrElse(TASKS))
  val rows = Var(0)
  val currentDataBagUI = Var(defaultDataBagUI.left.toOption)

  val settingTabs = Rx {
    currentDataBagUI().map {
      SettingTabs(_)
    }
  }
  val inputFilter = new InputFilter(currentDataBagUI().map {
    _.name()
  }.getOrElse(""))

  val factorySelector: Select[FactoryUI] = new Select("factories",
    Var(filter().factories),
    currentDataBagUI(),
    btn_primary, () ⇒ {
      currentDataBagUI().map { db ⇒
        db.dataUI() = factorySelector.content().map { f ⇒
          resetIODataUI(db, f)
          //FIXME: I am sure, there is a better idea than a cast...
          f.dataUI.asInstanceOf[db.DATAUI]
        }.get
      }
    })

  private val filters = Map[ConceptState, DataBagUI ⇒ Boolean](
    (ALL, db ⇒ inputFilter.contains(db.name())),
    (TASKS, db ⇒ isTaskUI(db) && inputFilter.contains(db.name())),
    (PROTOTYPES, db ⇒ isPrototypeUI(db) && inputFilter.contains(db.name()))
  )

  val conceptTable = bs.table(striped)(
    thead,
    Rx {
      val dbUIs: Seq[DataBagUI] = filter().factories.head
      tbody({
        val elements = for (db ← dbUIs.sortBy(_.name()) if filters(filter())(db)) yield {
          bs.tr(row)(
            bs.td(col_md_6)(a(dataBagUIView(db), cursor := "pointer", onclick := { () ⇒
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

  def dataBagUIView(db: DataBagUI) = db match {
    case proto: PrototypeDataBagUI ⇒ proto.name() + " [" + proto.dataUI().dimension().toString + "]"
    case _                         ⇒ db.name() + ""
  }

  val dimInput = bs.input("0")(placeholder := "Dim", width := 50, autofocus := true).render

  //New button
  val newGlyph =
    //FIXME: THE SIZE OF THE GLYPH IS SMALLER THAN THE REST OF THE GROUP WHEN GROUPEL
    // bs.button(glyph(glyph_plus))(onclick := { () ⇒ add
    bs.button("Add")(onclick := { () ⇒ add
    }).render

  def add = {
    val factory = filter().factories.head
    val dbUI = DataBagUI(factory)
    dbUI.name() = inputFilter.tag.value
    dimInput.value = "0"
    ClientService += dbUI
    setCurrent(dbUI)
    editionState() = true
  }

  def resetIODataUI(dbUI: DataBagUI, factory: FactoryUI) = dbUI match {
    case iodb: IODataBagUI ⇒
      iodb.inputDataUI() = DataBagUI.buildInput(factory.ioMapping)
      iodb.outputDataUI() = DataBagUI.buildOutput(factory.ioMapping)
    case _ ⇒
  }

  val conceptFilter = Rx {
    nav("filterNav", nav_pills, navItem("allfilter", "All", () ⇒ {
      filter() = ALL
      factorySelector.contents() = ClientService.factories
    }, filter() == ALL),
      navItem("valfilter", "Val", () ⇒ {
        filter() = PROTOTYPES
        factorySelector.contents() = ClientService.prototypeFactories
      }, filter() == PROTOTYPES),
      navItem("taskfilter", "Task", () ⇒ {
        filter() = TASKS
        factorySelector.contents() = ClientService.taskFactories
      }, filter() == TASKS),
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

  val saveButton = bs.button("Close", btn_test)(data("dismiss") := "modal", onclick := { () ⇒
    save
  })

  val dialog = {
    modalDialog(uuid,
      headerDialog(Rx {
        tags.div(
          nav(getID, navbar_form)(
            bs.form()(
              inputGroup(navbar_left)(
                inputFilter.tag,
                for (c ← prototypeExtraForm) yield c,
                if (editionState()) inputGroupButton(factorySelector.selector)
                else inputGroupButton(newGlyph)
              ),
              if (editionState()) {
                bs.span(navbar_right)(
                  saveHeaderButton
                )
              }
              else bs.span(navbar_right)(conceptFilter),
              onsubmit := { () ⇒
                if (editionState()) save
                else if (rows() == 0) add
              }
            )
          ))
      }),
      bodyDialog(Rx {
        tags.div(
          if (editionState()) {
            inputFilter.tag.value = currentDataBagUI().map {
              _.name()
            }.getOrElse((""))
            settingTabs() match {
              case Some(s: SettingTabs) ⇒ s.view
              case _                    ⇒ tags.div(h1("Create a  first data !"))
            }
          }
          else {
            inputFilter.tag.value = ""
            tags.div(conceptTable)
          }
        )
      }
      ),
      footerDialog(
        h2(saveButton)
      )
    )
  }.render

  def prototypeExtraForm: Seq[Modifier] = currentDataBagUI() match {
    case Some(db: DataBagUI) ⇒ db.dataUI() match {
      case p: PrototypeDataUI ⇒
        if (editionState()) {
          dimInput.value = p.dimension().toString
          Seq(inputGroupButton(style := "width:0px;"),
            dimInput)
        }
        else Seq()
      case _ ⇒ Seq()
    }
    case _ ⇒ Seq()
  }

  def save = {
    currentDataBagUI().map {
      db ⇒
        ClientService.setName(db, inputFilter.tag.value)
        //In case of prototype
        prototypeUI(db).map {
          _.dimension() = dimInput.value.toInt
        }
    }

    settingTabs().map {
      _.save
    }

    editionState() = false
    inputFilter.nameFilter() = ""
  }

}