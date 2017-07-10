package org.openmole.site

/*
 * Copyright (C) 11/05/17 // mathieu.leclaire@openmole.org
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

package object shared {
  lazy val searchDiv = "search-div"
  lazy val searchImg = "search-img"

  object profile {
    val button = "profileTrigger"
    val svgID = "profileSVG"
    val animation = "startProfileAnim"
  }
  object link {
    lazy val github = "https://github.com/openmole/openmole"
    lazy val demo = "http://demo.openmole.org"
    lazy val twitter = "https://twitter.com/OpenMOLE"
    lazy val blog = "blog.openmole.org"
    lazy val mailingList = "ask.openmole.org"
  }
}
