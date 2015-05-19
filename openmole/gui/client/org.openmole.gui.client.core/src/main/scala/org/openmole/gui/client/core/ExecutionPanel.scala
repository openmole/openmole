package org.openmole.gui.client.core

/*
 * Copyright (C) 17/05/15 // mathieu.leclaire@openmole.org
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

import org.openmole.core.workflow.mole.MoleExecution
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import bs._
import rx._

case class MoleExecutionUI(name: String, moleExecution: MoleExecution)

object ExecutionPanel {
  def apply() = new ExecutionPanel
}

class ExecutionPanel {

  val moleExecutionUIs: Var[Seq[MoleExecutionUI]] = Var(Seq())
  val currentMoleExecutionUI: Var[Option[MoleExecutionUI]] = Var(None)

  def setCurrent(mE: MoleExecutionUI) = currentMoleExecutionUI() = Some(mE)

  val executionTable = bs.table(striped)(
    thead,
    Rx {
      tbody({
        for (mE ← moleExecutionUIs()) yield {
          bs.tr(row)(
            bs.td(col_md_6)(tags.a(mE.name, cursor := "pointer", onclick := { () ⇒
              setCurrent(mE)
            })),
            bs.td(col_md_1)(bs.button(glyph(glyph_trash))(onclick := { () ⇒
              moleExecutionUIs() = moleExecutionUIs().filterNot { c ⇒
                c.moleExecution.id == mE.moleExecution.id
              }
            }
            ))
          )
        }
      }
      )
    }
  ).render

  val closeButton = bs.button("Close", btn_test)(data("dismiss") := "modal", onclick := { () ⇒
    println("Close")
  }
  )

  val dialog = modalDialog("executionPanelID",
    headerDialog(
      tags.div("Executions"
      ),
      bodyDialog(
        executionTable
      ),
      footerDialog(
        closeButton
      )
    )
  ).render

}