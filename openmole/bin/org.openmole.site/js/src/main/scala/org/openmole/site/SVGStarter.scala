package org.openmole.site

import org.scalajs.dom.raw._

import scalajs.js

/*
 * Copyright (C) 10/07/17 // mathieu.leclaire@openmole.org
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

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSGlobal
abstract class SVGAnimationElement extends org.scalajs.dom.SVGElement {

  /*
   * MDN
   *
   * Creates a begin instance time for the current time.
   * The new instance time is added to the begin instance times list. The behavior of this method is equivalent to beginElementAt(0)
   *
   */
  def beginElement(): Unit = js.native
}

object SVGStarter {

  def decorateTrigger(triggerID: String, animationID: String, timeOut: Int) = {
    val button = org.scalajs.dom.window.document.getElementById(triggerID)
    val animation = org.scalajs.dom.window.document.getElementById(animationID).asInstanceOf[SVGAnimationElement]

    val svgString = "position:absolute;margin-top:-70px"
    button.addEventListener("click", {
      (e: MouseEvent) =>
        animation.beginElement()
        button.setAttribute("style", "opacity:0;ponter:unset;" + svgString)
        org.scalajs.dom.window.setTimeout(() => button.setAttribute("style", "opacity:1;" + svgString), timeOut)
    })

  }
}
