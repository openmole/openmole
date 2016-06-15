package org.openmole.gui.misc.js

import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.stylesheet.all._
import bs._
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLSpanElement }
import rx._

import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import scalatags.JsDom.TypedTag

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
  type VisibleID = String
  type ExpandID = String
}

import Expander._

class Expander {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  private val expanded: Var[Map[ExpandID, Var[Boolean]]] = Var(Map())
  private val visible: Var[Map[ExpandID, Var[VisibleID]]] = Var(Map())

  def updateMaps(expandId: ExpandID, visibleId: VisibleID) = {
    if (!expanded.now.isDefinedAt(expandId)) expanded() = expanded.now.updated(expandId, Var(false))
    if (!visible.now.isDefinedAt(expandId)) visible() = visible.now.updated(expandId, Var(visibleId))
  }

  def isExpanded(id: ExpandID) = expanded.flatMap { _.getOrElse(id, Var(false)) }

  def isVisible(expandID: ExpandID, visibleID: VisibleID) = getVisible(expandID).map { _.exists { v ⇒ v == visibleID } }

  def getVisible(expandId: ExpandID) = isExpanded(expandId).map { ie ⇒
    if (ie) Some(visible.now(expandId).now)
    else None
  }

  def setTarget(expandId: ExpandID, visibleId: VisibleID) = {
    if (expanded.now(expandId).now) {
      if (visible.now(expandId).now == visibleId) {
        expanded.now(expandId)() = false
      }
      else {
        visible.now(expandId)() = visibleId
      }
    }
    else {
      visible.now(expandId)() = visibleId
      expanded.now(expandId)() = true
    }
  }

  def getLink(linkName: String, expandId: ExpandID, visibleId: VisibleID, todo: () ⇒ Unit = () ⇒ {}) = {
    updateMaps(expandId, visibleId)
    tags.span(cursor := "pointer", onclick := { () ⇒
      setTarget(expandId, visibleId)
      todo()
    })(linkName)

  }

  def getGlyph(glyph: ModifierSeq, linkName: String, expandId: ExpandID, visibleId: VisibleID, todo: () ⇒ Unit = () ⇒ {}) = {
    updateMaps(expandId, visibleId)
    tags.span(glyph, onclick := {
      () ⇒
        setTarget(expandId, visibleId)
        todo()
    }, linkName)
  }

}