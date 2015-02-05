package org.openmole.gui.ext.dataui

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

import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import scalatags.JsDom.all._
import scala.scalajs.js.annotation.JSExport
import org.openmole.gui.ext.dataui.PrototypeDataUI
import org.openmole.gui.misc.js.JsRxTags._
import rx._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

class InputPanelUI(dataUI: InputDataUI) extends PanelUI {

  val inputFilter = bs.input()(
    value := "",
    placeholder := "Filter",
    autofocus := "true"
  ).render

  //New button
  val newGlyph =
    //FIXME: THE SIZE OF THE GLYPH IS SMALLER THAN THE REST OF THE GROUP WHEN GROUPEL
    // bs.button(glyph(glyph_plus))(onclick := { () ⇒ add
    bs.button("Add")(onclick := { () ⇒
      println("Add proto")
    }).render

  val table = bs.table(
    thead(
      bs.tr()(
        bs.th("Name"),
        bs.th("Type"),
        bs.th("Default"),
        bs.th("Map to")
      ), tbody(Rx {
        for (i ← dataUI.inputsUI()) yield {
          bs.tr(row)(
            bs.td(col_md_4)(a(i.protoDataBagUI.name(),
              cursor := "pointer",
              onclick := { () ⇒
                {
                  println("click")
                }
              })),
            bs.td(col_md_2)(bs.label(i.protoDataBagUI.dataUI().dataType))
          /*  bs.td(col_md_5)(bs.label(db.dataUI().dataType, label_primary)),
                  bs.td(col_md_1)(bs.button(glyph(glyph_trash))(onclick := { () ⇒
                    ClientService -= db
                  }))*/
          )
        }
      }
      )
    )
  ).render

  @JSExport
  val view = bs.div()(
    bs.form()(
      bs.inputGroup(navbar_left)(
        inputFilter,
        bs.inputGroupButton(newGlyph)
      )
    ),
    table
  )

  def save = {
    //dataUI.truc() = trucInput.value
  }
}
