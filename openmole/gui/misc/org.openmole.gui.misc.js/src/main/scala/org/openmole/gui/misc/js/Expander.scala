package org.openmole.gui.misc.js

import org.scalajs.dom.raw.HTMLDivElement
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

class Expander {

  val hiddenDiv: Var[Map[String, Var[TypedTag[HTMLDivElement]]]] = Var(Map())
  private val expanded: Var[Map[String, Var[Boolean]]] = Var(Map())

  def isExpanded(id: String) = expanded().getOrElse(id, Var(false))()

  def getLink(linkName: String, id: String, suffixId: String, targetDiv: TypedTag[HTMLDivElement]) = {
    val linkID = id + suffixId
    if (!expanded().isDefinedAt(id)) expanded() = expanded().updated(id, Var(false))
    if (!hiddenDiv().isDefinedAt(id)) hiddenDiv() = hiddenDiv().updated(id, Var(tags.div()))

    tags.a(data("toggle") := "collapse", href := "#" + linkID, aria.expanded := expanded(), aria.controls := linkID, onclick := { () ⇒
      hiddenDiv()(id)() = targetDiv
      expanded()(id)() = !expanded()(id)()
    })(linkName)

  }
}