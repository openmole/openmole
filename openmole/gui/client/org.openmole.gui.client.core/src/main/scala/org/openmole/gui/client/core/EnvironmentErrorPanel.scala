package org.openmole.gui.client.core

import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.client._
import scalatags.JsDom.{ TypedTag, tags }
import scalatags.JsDom.all._
import org.openmole.gui.ext.tool.client.Utils
import org.scalajs.dom.raw.HTMLLabelElement
import rx._
import scaladget.bootstrapnative.Table.{ FixedCell, ReactiveRow, SubRow, VarCell }

/*
 * Copyright (C) 27/07/15 // mathieu.leclaire@openmole.org
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

class EnvironmentErrorPanel(environmentErrorData: EnvironmentErrorData) {

  val errorData = environmentErrorData.datedErrors.zipWithIndex.map {
    case (e, index) ⇒
      val id: scaladget.tools.ID = s"${e._1.environmentId}-$index}"
      id -> e
  }.toMap

  val detailOn: Var[Map[scaladget.tools.ID, Boolean]] = Var(Map())

  def toggleDetail(id: String) = {
    detailOn.update(detailOn.now.updated(id, !detailOn.now.get(id).getOrElse(false)))
  }

  def levelLabel(level: ErrorStateLevel) = label(level.name)(level match {
    case ErrorLevel() ⇒ label_danger
    case _            ⇒ label_warning
  })

  val view = {
    scaladget.bootstrapnative.Table(
      Rx(
        for {
          (id, (error, date, nb)) ← errorData.toSeq.sortBy(x ⇒ x._2._2).reverse
        } yield {
          ReactiveRow(
            id,
            Seq(
              FixedCell(tags.span(wordWrap := "break-word")(tags.a(error.errorMessage, pointer +++ Seq(fontSize := 13, scalatags.JsDom.all.color := "#222", textDecoration.underline), onclick := { () ⇒ toggleDetail(id) })), 0),
              FixedCell(levelLabel(error.level)(badge(nb.toString, environmentErrorBadge)), 1),
              FixedCell(span(fontSize := 13, textCenter)(Utils.longToDate(date)), 2)
            ), Seq(borderTop := "2px solid white", backgroundColor := "#ff000033")
          )
        }),
      subRow = Some((i: scaladget.tools.ID) ⇒ SubRow(
        Rx {
          val stackText = scrollableText()
          stackText.setContent(errorData.get(i).map { _._1.stack.stackTrace }.getOrElse(""))
          div(stackText.sRender)
        }, detailOn.map {
          _.get(i).getOrElse(false)
        }
      )
      )).render(width := "100%", marginTop := 30)
  }

}

