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
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared.Api
import org.scalajs.jquery
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.js.timers._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.ext.data._
import autowire._
import bs._
import rx._

object ExecutionPanel {
  def apply = new ExecutionPanel
}

class ExecutionPanel extends ModalPanel {
  val modalID = "executionsPanelID"

  val moleExecutionUIs: Var[Seq[(ExecutionId, States)]] = Var(Seq())
  val currentMoleExecutionUI: Var[Option[ExecutionId]] = Var(None)
  val intervalHandler: Var[Option[SetIntervalHandle]] = Var(None)

  def onOpen = () ⇒ {
    intervalHandler() = Some(setInterval(3000) {
      allStates
    })
  }

  def onClose = () ⇒ {
    intervalHandler().map {
      clearInterval
    }
  }

  def allStates = Post[Api].allStates().call().foreach { c ⇒
    moleExecutionUIs() = c
    c.foreach {
      case (id, states) ⇒
        println("ID " + id + " // " + states.running.running + " // " + states.running.completed)
    }
  }

  def setCurrent(id: ExecutionId) = currentMoleExecutionUI() = Some(id)

  val executionTable = bs.table(striped)(
    thead,
    Rx {
      tbody({
        for (mE ← moleExecutionUIs()) yield {
          bs.tr(row)(
            bs.td(col_md_6)(tags.a(mE._1.id, cursor := "pointer", onclick := { () ⇒
              setCurrent(mE._1)
            })),
            bs.td(col_md_1)(bs.button(glyph(glyph_trash))(onclick := { () ⇒
              moleExecutionUIs() = moleExecutionUIs().filterNot {
                case (id, _) ⇒
                  id.id == mE._1.id
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

  val dialog = modalDialog(modalID,
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
  )

  jquery.jQuery(org.scalajs.dom.document).on("hide.bs.modal", "#" + modalID, () ⇒ {
    onClose()
  })
}