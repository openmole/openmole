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
  lazy val blogposts = "blog-posts"
  lazy val newsPosts = "news-posts"

  object profile {
    val button = "profileTrigger"
    val animation = "startProfileAnim"
  }

  object pse {
    val button = "pseTrigger"
    val animation = "startPseAnim"
  }

  object sensitivity {
    val button = "sensitivityTrigger"
    val animation = "startSensitivityAnim"
  }

  object link {
    lazy val demo = "https://demo.openmole.org"
    lazy val twitter = "https://twitter.com/OpenMOLE"
    lazy val blog = "https://blog.openmole.org"
    lazy val simpluDemo = "https://simplu.openmole.org"
    lazy val mailingList = "https://ask.openmole.org"

    object partner {
      lazy val iscpif = "http://iscpif.fr"
      lazy val parisgeo = "http://www.parisgeo.cnrs.fr/"
      lazy val biomedic = "http://biomedic.doc.ic.ac.uk/"
      lazy val idf = "https://www.iledefrance.fr/"
      lazy val paris = "https://www.paris.fr/"
      lazy val ign = "http://www.ign.fr/"
    }

    object repo {
      lazy val openmole = "https://github.com/openmole/openmole"
      lazy val gridscale = "https://github.com/openmole/gridscale"
      lazy val scaladget = "https://github.com/openmole/scaladaget"
      lazy val scalawui = "https://github.com/openmole/scalaWUI"
      lazy val mgo = "https://github.com/openmole/mgo"
      lazy val simplu = "https://github.com/IGNF/simplu3D"
    }

  }

  def rawFrag(content: String) = {
    val builder = new scalatags.text.Builder()
    scalatags.Text.all.raw(content).applyTo(builder)
    builder.children.head
  }
}
