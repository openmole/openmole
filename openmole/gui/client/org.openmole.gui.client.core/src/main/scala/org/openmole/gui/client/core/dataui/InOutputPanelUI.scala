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
import org.openmole.gui.misc.js.{ Forms ⇒ bs, InputFilter }
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom.{ tags }
import IOPanelUIUtil._

class InOutputPanelUI(val panel: GenericPanel, val dataUI: InOutputDataUI) extends PanelUI {
  val inputFilter = InputFilter(pHolder = "Add a prototype", inputID = InputFilter.filterId)

  def filteredInputsUI = filtered(inputFilter, dataUI, dataUI.mappingsFactory)

  //New button
  val newGlyph =
    //FIXME: THE SIZE OF THE GLYPH IS SMALLER THAN THE REST OF THE GROUP WHEN GROUPEL
    // bs.button(glyph(glyph_plus))(onclick := { () ⇒ add
    bs.button("Add")(`type` := "submit", onclick := { () ⇒
      val filtering = filteredInputsUI
      val inputValue = inputFilter.nameFilter()
      if (filtering.size == 1) {
        add(filtering.head.protoDataBagUI)
      }
      else if (!inputValue.isEmpty && !ClientService.existsPrototype(inputValue)) {
        val newProto = buildProto(inputValue)
        setCurrent(newProto)
        add(newProto)
      }
    }).render

  def view = {
    bs.form(spacer20)(
      bs.formGroup( /*row + */ col_md_12)(
        bs.inputGroup(col_md_6 + col_md_offset_3)(
          inputFilter.tag,
          bs.inputGroupButton(newGlyph)
        )),
      bs.formGroup(col_md_12)(Rx {
        (for ((headers, inputsUI) ← (filteredInputsUI ++ dataUI.inoutputsUI()).groupBy { i ⇒ dataUI.mappingKeys(i.protoDataBagUI) }) yield {
          bs.table(col_md_12 + striped)(
            buildHeaders(prototypeHeaderSequence ++ headers :+ ""),
            tbody(
              for (i ← inputsUI.sortBy(_.protoDataBagUI.name())) yield {
                coloredTR((buildPrototypeTableView(i, () ⇒ setCurrent(i.protoDataBagUI)) :+
                  delButtonTD(() ⇒ dataUI -= i)
                ), () ⇒ !dataUI.inoutputsUI().contains(i),
                  () ⇒ add(i))
              }
            )
          ).render
        }
        ).toSeq
      }
      )
    )
  }

  def add(io: InOutputUI): Unit = add(io.protoDataBagUI)

  def add(pdb: PrototypeDataBagUI): Unit = {
    dataUI += pdb
    inputFilter.clear
  }

  def setCurrent(pdb: PrototypeDataBagUI) = {
    save
    panel.currentDataBagUI().map {
      db ⇒
        panel.stack(db, dataUI match {
          case i: InputDataUI  ⇒ 1
          case i: OutputDataUI ⇒ 2
        })
    }
    panel.setCurrent(pdb)
  }

  def save = saveInOutputsUI(dataUI.inoutputsUI())

}
