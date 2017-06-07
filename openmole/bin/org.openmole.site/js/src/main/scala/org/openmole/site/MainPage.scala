package org.openmole.site

/*
 * Copyright (C) 12/05/17 // mathieu.leclaire@openmole.org
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

import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.{ all ⇒ sheet }
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import sheet._
import bs._

object MainPage {
  val replacer = utils.replacer

  def load(page: JSPage) = {

    val main = tags.div(sitesheet.mainDiv)(
      replacer.tag
    )

    org.scalajs.dom.window.document.body.appendChild(main)
    replacer.replaceWith(shared.sitexMain)
  }

}
