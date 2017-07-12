package org.openmole.site

import scalatags.Text.all._
import scalatags.generic.StylePair

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
    val animation = "startProfileAnim"
  }

  object link {
    lazy val iscpif = "http://iscpif.fr"
    lazy val parisgeo = "http://www.parisgeo.cnrs.fr/"
    lazy val biomedic = "http://biomedic.doc.ic.ac.uk/"
    lazy val github = "https://github.com/openmole/openmole"
    lazy val demo = "http://demo.openmole.org"
    lazy val twitter = "https://twitter.com/OpenMOLE"
    lazy val blog = "blog.openmole.org"
    lazy val simpluDemo = "https://simplu.openmole.org"
    lazy val mgo = "https://github.com/openmole/mgo"
    lazy val gridscale = "https://github.com/openmole/gridscale"
    lazy val scaladget = "https://github.com/openmole/scaladaget"
    lazy val scalawui = "https://github.com/openmole/scalaWUI"
    lazy val mailingList = "ask.openmole.org"
  }
}
