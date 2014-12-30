package org.openmole.gui.misc.js

/*
 * Copyright (C) 05/12/14 // mathieu.leclaire@openmole.org
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

import org.scalajs.dom.HTMLSelectElement
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.scalajs.jquery.jQuery
import rx._

abstract class GenericAutoInput[T](autoID: String, contents: Seq[T], default: Option[T] = None, placeHolder: Option[String]) {

  def selector: TypedTag[HTMLSelectElement]

  protected lazy val firstInd = default match {
    case None ⇒ 0
    case _ ⇒
      val ind = contents.indexOf(default.get)
      if (ind != -1) ind else 0
  }

  lazy val content: Var[T] = Var(contents(firstInd))
}

class AutoInput[T <: DisplayableRx with Identifiable](autoID: String, contents: Seq[T], default: Option[T] = None, placeHolder: Option[String]) extends GenericAutoInput[T](autoID, contents, default, placeHolder) {

  def jQid = "#" + autoID

  val selector = select(id := autoID,
    placeholder := placeHolder.getOrElse(""),
    onchange := { () ⇒ applyOnChange }
  )(
      contents.map { c ⇒
        option(value := c.uuid)(c.name())
      }.toSeq: _*
    )

  def applyOnChange: Unit = {
    val ind = jQuery(jQid).find("option:selected").index()
    content() = contents(ind)
  }
}