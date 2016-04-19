package org.openmole.gui.client.core

import org.openmole.gui.ext.data._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.{ stylesheet, Utils }
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import bs._

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

  case class SelectableLevel(level: ErrorStateLevel, name: String, uuid: String = Utils.getUUID)

  implicit def errorStateLevelToSelectableLevel(level: ErrorStateLevel): SelectableLevel = SelectableLevel(level, level.name)

}

class EnvironmentErrorPanel {

  val scrollable = scrollableDiv()

  private def entries(errors: Seq[(EnvironmentError, Int)]) = tags.table(
    for { (error, nb) ← errors } yield {
      tags.tr(
        tags.a(s"${error.errorMessage} ($nb, ${Utils.longToDate(error.date)})", cursor := "pointer", onclick := {
          () ⇒
            panels.environmentStackPanel.content() = error.stack.stackTrace
            panels.environmentStackTriggerer.open
        })
      )
    }
  )

  def setErrors(ers: Seq[(EnvironmentError, Int)]) = scrollable.setChild(div(stylesheet.environmentPanelError)(entries(ers)).render)

  val view = scrollable.sRender

}
