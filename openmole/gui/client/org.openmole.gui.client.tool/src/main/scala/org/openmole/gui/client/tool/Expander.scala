package org.openmole.gui.client.tool

import scaladget.tools._

import rx._
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._

/*
 * Copyright (C) 09/06/15 // mathieu.leclaire@openmole.org
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

object Expander {
  type ColumnID = String
  type ExpandID = String
}

import Expander._

class Expander(val expandID: ExpandID) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val expanded: Var[Boolean] = Var(false)
  val visibleColumn: Var[Option[ColumnID]] = Var(None)
  val currentColumn: Var[Option[ColumnID]] = Var(None)

  expanded.trigger {
    setCurrentColumn
  }

  visibleColumn.trigger {
    setCurrentColumn
  }

  private def setCurrentColumn = currentColumn() = {
    if (expanded.now) visibleColumn.now
    else None
  }

  def close = {
    expanded() = false
    visibleColumn() = None
    currentColumn() = None
  }

  def update(columnID: ColumnID) = {

    val currExpanded = expanded.now
    val currColumn = visibleColumn.now

    if (currExpanded) {
      if (Some(columnID) == currColumn) expanded() = false
    }
    else expanded() = true
    visibleColumn() = Some(columnID)

  }

  def isVisible(columnID: ColumnID) =
    visibleColumn.now.map { c ⇒
      c == columnID
    }.getOrElse(false)

  def getLink(linkName: String, columnID: ColumnID, todo: () ⇒ Unit = () ⇒ {}) = {
    tags.span(cursor := "pointer", onclick := { () ⇒
      update(columnID)
      todo()
    })(linkName)
  }

  def getGlyph(glyph: ModifierSeq, linkName: String, columnID: ColumnID, todo: () ⇒ Unit = () ⇒ {}) = {
    tags.span(glyph, onclick := {
      () ⇒
        update(columnID)
        todo()
    }, linkName)
  }

}