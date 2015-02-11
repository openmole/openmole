package org.openmole.gui.client.service.dataui

/*
 * Copyright (C) 29/01/2015 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.service.ClientService
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.{ InputFilter, Forms ⇒ bs }
import rx._

import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import org.openmole.gui.ext.dataui.PanelUI

class InputPanelUI(dataUI: InputDataUI) extends PanelUI {

  val inputFilter = new InputFilter(pHolder = "Add a prototype")

  //New button
  val newGlyph =
    //FIXME: THE SIZE OF THE GLYPH IS SMALLER THAN THE REST OF THE GROUP WHEN GROUPEL
    // bs.button(glyph(glyph_plus))(onclick := { () ⇒ add
    bs.button("Add")(`type` := "submit", onclick := { () ⇒
      if (filteredInputsUI.size == 1) {
        dataUI += filteredInputsUI.head
        inputFilter.clear
      }
    }).render

  val table = bs.table(col_md_12)(
    thead(
      tags.tr(
        tags.th("Name"),
        tags.th("Type"),
        tags.th("Dimension"),
        tags.th("Default"),
        tags.th("Map to"),
        tags.th("")
      )), Rx {
      tbody(
        for (
          i ← (dataUI.inputsUI() ++ filteredInputsUI).sortBy(_.protoDataBagUI.name())
        ) yield {
          tags.tr(
            bs.td(col_md_3)(a(i.protoDataBagUI.name(),
              cursor := "pointer",
              onclick := { () ⇒
                {
                  println("click")
                }
              })),
            bs.td(col_md_2)(bs.label(i.protoDataBagUI.dataUI().dataType, label_primary)),
            bs.td(col_md_2)(tags.span(i.protoDataBagUI.dataUI().dimension)),
            bs.td(col_md_2)(i.default().getOrElse("").toString),
            bs.td(col_md_2),
            bs.td(col_md_1)(bs.button(glyph(glyph_minus))(onclick := { () ⇒
              dataUI -= i
            }))
          /*  bs.td(col_md_5)(bs.label(db.dataUI().dataType, label_primary)),
                                  bs.td(col_md_1)(bs.button(glyph(glyph_trash))(onclick := { () ⇒
                                    ClientService -= db
                                  }))*/
          )
        }
      )
    }
  ).render

  def filteredInputsUI = ClientService.prototypeDataBagUIs.map { p ⇒ inputUI(p) }.filter { i ⇒
    inputFilter.contains(i.protoDataBagUI.name()) &&
      !inputFilter.nameFilter().isEmpty &&
      !dataUI.exists(i)
  }

  @JSExport
  val view =
    bs.form(spacer20)(
      bs.formGroup( /*row + */ col_md_12)(
        bs.inputGroup(col_md_6 + col_md_offset_3)(
          inputFilter.tag,
          bs.inputGroupButton(newGlyph)
        )),
      bs.formGroup(col_md_12)(table)
    )

  def save = {
    //dataUI.inputsUI() =
    //dataUI.truc() = trucInput.value
  }
}
