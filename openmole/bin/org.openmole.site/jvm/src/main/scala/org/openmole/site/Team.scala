package org.openmole.site

import scalatags.Text.{ TypedTag, tags2 }
import scalatags.Text.all._
import stylesheet._
import scalatex.{ openmole ⇒ scalatex }

/*
 * Copyright (C) 03/07/17 // mathieu.leclaire@openmole.org
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

object Team {

  val shortStyle = Seq(
    fontSize := "14px",
    color := DARK_GREY,
    fontStyle := "italic"
  )

  def member(image: FileResource, name: String): Frag =
    div(
      img(src := image.file, height := 100, paddingBottom := 5),
      span(name, memberStyle)
    )

  scalatex.WhoAreWe

  def build = {
    //    div(
    //      member(Resource.img.romain, "Romain Reuillon",
    //        div(
    //          span("CNRS Researcher at Géographie-cité (UMR 8504) and ISC-PIF (UPS 3611). Scientific Manager of the OpenMOLE Platform, " +
    //            "leader of the development for Scientific simulation and computing, he developped both "),
    //          tools.to("https://github.com/openmole/mgo", "MGO"),
    //          " and ",
    //          tools.to("https://github.com/openmole/gridscale", "Gridscale"),
    //          " libraries."
    //        )),
    //      member(Resource.img.mathieu, "Mathieu Leclaire",
    //        div(
    //          s"""CNRS Researcher Engineer at ISC-PIF (UPS 3611) and Géographie-cité (UMR 8504)
    //        " Developper of the OpenMOLE Platform,
    //        s"he developped both the ${tools.to("https://github.com/openmole/scaladaget", "Scaladget")} library and
    //        ${tools.to("https://github.com/openmole/scalaWUI", "ScalaWUI")} skeleton."""
    //        ))
    //    )

  }

}
