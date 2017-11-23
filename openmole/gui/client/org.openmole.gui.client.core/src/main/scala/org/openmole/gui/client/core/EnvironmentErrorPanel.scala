package org.openmole.gui.client.core

import scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.ext.data._
import scaladget.api.{ BootstrapTags ⇒ bs }
import org.scalajs.dom.html.TableSection
import org.openmole.gui.ext.tool.client.JsRxTags._
import org.openmole.gui.ext.tool.client._

import scalatags.JsDom.{ TypedTag, tags }
import scalatags.JsDom.all._
import sheet._
import bs._
import scaladget.api.BootstrapTags.ScrollableTextArea.NoScroll
import org.openmole.gui.ext.tool.client.Utils
import org.scalajs.dom.raw.HTMLLabelElement
import rx._

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

class EnvironmentErrorPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val scrollableTable = scrollableDiv()
  val scrollableStack = scrollableText()
  val sortingAndOrdering: Var[ListSortingAndOrdering] = Var(ListSortingAndOrdering(TimeSorting(), Descending()))
  val currentData: Var[Option[EnvironmentErrorData]] = Var(None)

  val topTriangle = glyph_triangle_top +++ (fontSize := 10)
  val bottomTriangle = glyph_triangle_bottom +++ (fontSize := 10)

  def exclusiveButton(title: String, action1: () ⇒ Unit, action2: () ⇒ Unit) = exclusiveButtonGroup(emptyMod)(
    ExclusiveButton.twoGlyphSpan(
      topTriangle,
      bottomTriangle,
      action1,
      action2,
      preString = title
    )
  ).div

  def setSorting(sorting: ListSorting, ordering: ListOrdering) = {
    sortingAndOrdering() = ListSortingAndOrdering(sorting, ordering)
    currentData.now.foreach {
      setErrors
    }
  }

  def sort(datedErrors: EnvironmentErrorData, sortingAndOrdering: ListSortingAndOrdering): Seq[(String, Long, Int, ErrorStateLevel, Error)] = {
    val lines =
      for {
        (error, mostRecentDate, occurrences) ← datedErrors.datedErrors
      } yield (error.errorMessage, mostRecentDate, occurrences, error.level, error.stack)

    val sorted = sortingAndOrdering.fileSorting match {
      case AlphaSorting() ⇒ lines.sortBy(_._1)
      case TimeSorting()  ⇒ lines.sortBy(_._2)
      case _              ⇒ lines.sortBy(_._4.name)
    }

    sortingAndOrdering.fileOrdering match {
      case Ascending() ⇒ sorted
      case _           ⇒ sorted.reverse
    }
  }

  case class Line(message: String, stack: String, date: String, occurrences: String, levelLabel: TypedTag[HTMLLabelElement]) {
    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
    val detailOn = Var(false)

    def toggleDetails = {
      detailOn() = !detailOn.now
    }

    val render = tags.tr(row)(
      tags.td(colMD(9), wordWrap := "break-word")(tags.a(message, pointer +++ (fontSize := 13), onclick := { () ⇒ toggleDetails })), //(width := 400)
      tags.td(colMD(1) +++ textCenter)(bs.badge(occurrences, environmentErrorBadge)),
      tags.td(colMD(1) +++ (fontSize := 13) +++ textCenter)(date),
      tags.td(colMD(1) +++ textCenter)(levelLabel)
    )
  }

  def setErrors(ers: EnvironmentErrorData) = {
    val someErs = Some(ers)
    if (someErs != currentData.now) {
      currentData() = someErs
    }
  }

  def levelLabel(level: ErrorStateLevel) = label(level.name)(level match {
    case ErrorLevel() ⇒ label_danger
    case _            ⇒ environmentErrorBadge +++ "label"
  })

  val view = {
    val errorTable = tags.table(sheet.table +++ ms("EnvError") +++ (width := "100%"))(
      thead(
        tr(row)(
          th(exclusiveButton("Error", () ⇒ setSorting(AlphaSorting(), Ascending()), () ⇒ setSorting(AlphaSorting(), Descending()))),
          th(""),
          th(exclusiveButton("Date", () ⇒ setSorting(TimeSorting(), Ascending()), () ⇒ setSorting(TimeSorting(), Descending()))),
          th(exclusiveButton("Level", () ⇒ setSorting(LevelSorting(), Ascending()), () ⇒ setSorting(LevelSorting(), Descending())))
        )
      ), Rx {
        tbody(
          for {
            (message, date, occurrences, level, stack) ← sort(currentData().getOrElse(EnvironmentErrorData.empty), sortingAndOrdering.now)
          } yield {

            val line = Line(message, stack.stackTrace, Utils.longToDate(date), occurrences.toString, levelLabel(level))
            val stackText = scrollableText()
            stackText.setContent(line.stack)
            Seq(
              line.render,
              tr(
                td(colMD(12) +++ (fontSize := 11))(
                  colspan := 12,
                  Rx {
                    if (line.detailOn()) {
                      tags.div(
                        stackText.view
                      )
                    }
                    else tags.div()
                  }
                )
              )
            )
          }
        )
      }
    )

    scrollableTable.setChild(div(omsheet.environmentPanelError)(errorTable).render)
    scrollableTable.sRender
  }

}

