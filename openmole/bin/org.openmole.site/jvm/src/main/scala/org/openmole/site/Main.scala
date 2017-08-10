package org.openmole.site

import scalatags.Text.all._
import org.openmole.site.tools._
import stylesheet._

/*
 * Copyright (C) 28/06/17 // mathieu.leclaire@openmole.org
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

object Main {

  private def readMore(page: Page) = pageLinkButton("Read more", page, false, Seq(classIs(btn ++ btn_default), marginTop := 20))

  def build = {
    div(classIs(row), paddingTop := 100)(
      div(classIs(colMD(4)))(
        div(centerBox)(
          img(src := Resource.img.model.code.file, width := 120),
          div(mainTitle, "RUN"),
          div(mainText, "OpenMOLE runs your own program, whatever the language. Java, Binary exe, NetLogo, R, SciLab, Python, C++..."),
          readMore(DocumentationPages.model)
        )
      ),
      div(classIs(colMD(4)))(
        div(centerBox)(
          img(src := Resource.img.method.exploreMap.file, width := 120),
          div(mainTitle, "EXPLORE"),
          div(mainText, "Explore spaces of parameters, optimize, test the sensitivity of your model through innovative and integrated methods"),
          readMore(DocumentationPages.method)
        )
      ),
      div(classIs(colMD(4)))(
        div(centerBox)(
          img(src := Resource.img.environment.scale.file, width := 120),
          div(mainTitle, "SCALE"),
          div(mainText, "Scale up your experimentations with no effort on servers, clusters, grid, cloud, ..."),
          readMore(DocumentationPages.environment)
        )
      )
    )
  }
}
