package org.openmole.gui.client.core

import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.Utils
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import bs._
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

object EnvironmentErrorPanel {

  case class SelectableLevel(level: ErrorStateLevel, name: String, uuid: String = uuID)

  implicit def errorStateLevelToSelectableLevel(level: ErrorStateLevel): SelectableLevel = SelectableLevel(level, level.name)

}

class EnvironmentErrorPanel {

  val expandedError: Var[Option[EnvironmentError]] = Var(None)
  val scrollable = scrollableDiv()

  def entries(errors: Seq[EnvironmentError]) = tags.table(
    (for {
      error ← errors.groupBy(e ⇒ (e.errorMessage, e.stack)).values.map {
        _.sortBy(_.date).last
      }.toSeq
      nb = errors.count(_.copy(date = 0L) == error.copy(date = 0L))
    } yield {
      Seq(
        tags.tr(
          tags.a(s"${error.errorMessage} ($nb, ${Utils.longToDate(error.date)})", cursor := "pointer", onclick := {
            () ⇒
              panels.environmentStackPanel.content() = error.stack.stackTrace
              panels.environmentStackTriggerer.open
              expandedError() = expandedError() match {
                case None ⇒ Some(error)
                case _    ⇒ None
              }
          })
        ) /*,
        tags.tr(
          expandedError().map { expanded ⇒
            tags.div(
              if (expanded == error) expanded.stack.stackTrace
              else ""
            )
          }
        )*/
      )
    }).flatten: _*
  )

  def setErrors(ers: Seq[EnvironmentError]) = scrollable.setChild(bs.div("environmentPanelError")(entries(ers)).render)

  val view = scrollable.sRender

}
