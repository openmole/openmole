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

import scalatags.JsDom.tags._
import scalatags.JsDom.short._
import scalatags.JsDom.attrs._
import org.openmole.gui.ext.aspects._
import org.scalajs.jquery.jQuery
import scala.scalajs.js
import rx._

class AutoInput[T <: Displayable with Identifiable](autoID: String, placeHolder: String, contents: Seq[T]) {

  val mapping: Map[Int, T] = contents.zipWithIndex.map { case (v, ind) ⇒ ind -> v }.toMap
  //contents.map { c ⇒ c.id -> c }.toMap

  val selector = select(id := autoID,
    onchange := { () ⇒ applyOnChange })(
      contents.map { c ⇒
        option(value := c.id)(c.name)
      }.toSeq: _*
    )

  private val selectorRender = selector.render

  println("firste seleceted " + mapping(selectorRender.selectedIndex).name)
  val content: Var[T] = Var(mapping(selectorRender.selectedIndex))

  def applyOnChange: Unit = {
    val ind = jQuery("#" + autoID).find("option:selected").index()
    println("jqauery : " + ind)
    content() = mapping(ind)
  }
}