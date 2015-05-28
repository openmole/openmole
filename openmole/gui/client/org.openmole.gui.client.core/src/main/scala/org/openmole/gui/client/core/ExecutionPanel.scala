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
import org.scalajs.dom.html.Anchor
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import bs._
import rx._

object ExecutionPanel {
  def apply(triggerLink: Anchor) = new ExecutionPanel(triggerLink)
}

class ExecutionPanel(private val triggerLink: Anchor) {

  val moleExecutionUIs: Var[Seq[String]] = Var(Seq())
  val currentMoleExecutionUI: Var[Option[String]] = Var(None)

  def ++(id: String) = moleExecutionUIs() = moleExecutionUIs() :+ id

  def setCurrent(id: String) = currentMoleExecutionUI() = Some(id)

  val executionTable = bs.table(striped)(
    thead,
    Rx {
      tbody({
        for (mE ← moleExecutionUIs()) yield {
          bs.tr(row)(
            bs.td(col_md_6)(tags.a(mE, cursor := "pointer", onclick := { () ⇒
              setCurrent(mE)
            })),
            bs.td(col_md_1)(bs.button(glyph(glyph_trash))(onclick := { () ⇒
              moleExecutionUIs() = moleExecutionUIs().filterNot { id ⇒
                id == mE
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

  def trigger = triggerLink.click

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