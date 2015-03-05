package org.openmole.gui.misc.js

/*
 * Copyright (C) 13/01/15 // mathieu.leclaire@openmole.org
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

import rx._
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.JsRxTags._

object Select {
  def apply[T <: Displayable with Identifiable](autoID: String,
                                                contents: Seq[T],
                                                default: Option[T],
                                                key: ClassKeyAggregator = Forms.emptyCK,
                                                onclickExtra: () ⇒ Unit = () ⇒ {}) = new Select(autoID, Var(contents), default, key, onclickExtra)
}

class Select[T <: Displayable with Identifiable](autoID: String,
                                                 val contents: Var[Seq[T]],
                                                 default: Option[T] = None,
                                                 key: ClassKeyAggregator = Forms.emptyCK,
                                                 onclickExtra: () ⇒ Unit = () ⇒ {}) {

  val content: Var[Option[T]] = Var(contents().size match {
    case 0 ⇒ None
    case _ ⇒ default match {
      case None ⇒ Some(contents()(0))
      case _ ⇒
        val ind = contents().indexOf(default.get)
        if (ind != -1) Some(contents()(ind)) else Some(contents()(0))
    }
  })

  val selector = Forms.buttonGroup()(
    a(
      `class` := "btn " + key.key + " dropdown-toggle", "data-toggle".attr := "dropdown", href := "#")(
        Rx {
          content().map {
            _.name
          }.getOrElse(contents()(0).name) + " "
        },
        span(`class` := "caret")
      ).render,
    ul(`class` := "dropdown-menu", id := autoID)(
      Rx {
        for (c ← contents().zipWithIndex) yield {
          scalatags.JsDom.tags.li(a(
            href := "#", onclick := { () ⇒
              content() = Some(contents()(c._2))
              onclickExtra()
            })(c._1.name)
          )
        }
      }
    )
  ).render
}
