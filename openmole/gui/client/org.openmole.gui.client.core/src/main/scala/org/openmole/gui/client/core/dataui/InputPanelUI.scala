package org.openmole.gui.client.core.dataui

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

import org.openmole.gui.client.core.{ ClientService, GenericPanel }
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.{ InputFilter, Forms ⇒ bs }
import rx._

import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import scalatags.JsDom.tags

class InputPanelUI(panel: GenericPanel, dataUI: InputDataUI) extends PanelUI {
  val inputFilter = new InputFilter(pHolder = "Add a prototype")

  def filteredInputsUI = ClientService.prototypeDataBagUIs.map { p ⇒ inputUI(p) }.filter { i ⇒
    inputFilter.contains(i.protoDataBagUI.name()) &&
      !inputFilter.nameFilter().isEmpty &&
      !dataUI.exists(i)
  }

  //New button
  val newGlyph =
    //FIXME: THE SIZE OF THE GLYPH IS SMALLER THAN THE REST OF THE GROUP WHEN GROUPEL
    // bs.button(glyph(glyph_plus))(onclick := { () ⇒ add
    bs.button("Add")(`type` := "submit", onclick := { () ⇒
      if (filteredInputsUI.size == 1) {
        val head = filteredInputsUI.head
        dataUI += head.protoDataBagUI
        inputFilter.clear
      }
    }).render

  @JSExport
  val view =
    bs.form(spacer20)(
      bs.formGroup( /*row + */ col_md_12)(
        bs.inputGroup(col_md_6 + col_md_offset_3)(
          inputFilter.tag,
          bs.inputGroupButton(newGlyph)
        )),
      bs.formGroup(col_md_12)(Rx {
        (for ((headers, inputsUI) ← (filteredInputsUI ++ dataUI.inputsUI()).groupBy { i ⇒ dataUI.mappingKeys(i.protoDataBagUI) }) yield {
          bs.table(col_md_12 + striped)(
            thead(
              tags.tr(
                tags.th("Name"),
                tags.th("Type"),
                tags.th("Dim"),
                for (k ← headers) yield {
                  tags.th(k)
                },
                tags.th("")
              )
            ),
            tbody(
              for (i ← inputsUI.sortBy(_.protoDataBagUI.name())) yield {
                bs.tr(
                  if (dataUI.inputsUI().contains(i)) nothing
                  else warning
                )(
                    bs.td(col_md_2)(a(i.protoDataBagUI.name(),
                      cursor := "pointer",
                      onclick := { () ⇒
                        {
                          save
                          panel.currentDataBagUI().map { db ⇒ panel.stack(db, 1) }
                          panel.setCurrent(i.protoDataBagUI)
                        }
                      })),
                    bs.td(col_md_1)(bs.label(i.protoDataBagUI.dataUI().dataType, label_primary)),
                    bs.td(col_md_1)(tags.span(i.protoDataBagUI.dataUI().dimension)),
                    for (
                      f ← i.mappings.fields.map {
                        _.panelUI
                      }
                    ) yield {
                      tags.td(f.view)
                    },
                    bs.td(col_md_1)(bs.button(glyph(glyph_minus))(onclick := { () ⇒
                      dataUI -= i
                    }))
                  )
              }
            )
          )
        }).toSeq
      }
      )
    )

  def save = {
    dataUI.inputsUI().map {
      _.mappings.fields.map {
        _.panelUI.save
      }
    }
  }
}
