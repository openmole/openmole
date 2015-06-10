package org.openmole.gui.misc.js

import org.scalajs.dom.raw.HTMLDivElement
import rx._

import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
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

  private val expanded: Var[Map[ExpandID, Var[Boolean]]] = Var(Map())
  private val visible: Var[Map[ExpandID, Var[VisibleID]]] = Var(Map())

  def updateMaps(expandId: ExpandID, visibleId: VisibleID) = {
    if (!expanded().isDefinedAt(expandId)) expanded() = expanded().updated(expandId, Var(false))
    if (!visible().isDefinedAt(expandId)) visible() = visible().updated(expandId, Var(visibleId))
  }

  def isExpanded(id: ExpandID) = expanded().getOrElse(id, Var(false))()

  def getVisible(expandId: ExpandID): Option[VisibleID] = if (isExpanded(expandId)) {
    Some(visible()(expandId)())
  }
  else None

  def setTarget(expandId: ExpandID, visibleId: VisibleID) = {
    if (expanded()(expandId)()) {
      if (visible()(expandId)() == visibleId) {
        expanded()(expandId)() = false
      }
      else {
        visible()(expandId)() = visibleId
      }
    }
    else {
      visible()(expandId)() = visibleId
      expanded()(expandId)() = true
    }
  }

  def getLink(linkName: String, expandId: ExpandID, visibleId: VisibleID) = {
    updateMaps(expandId, visibleId)
    tags.span(cursor := "pointer", onclick := {
      () ⇒ setTarget(expandId, visibleId)
    })(linkName)

  }

  def getGlyph(glyph: ClassKeyAggregator, linkName: String, expandId: ExpandID, visibleId: VisibleID) = {
    updateMaps(expandId, visibleId)
    bs.glyphSpan(glyph, () ⇒ setTarget(expandId, visibleId), linkName)
  }
}