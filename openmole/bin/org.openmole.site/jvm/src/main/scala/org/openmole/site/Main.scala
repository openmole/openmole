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

    def runText = "OpenMOLE runs your own program, whatever the language. Java, Binary exe, NetLogo, R, SciLab, Python, C++..."
    def exploreText = "Explore spaces of parameters, optimize, test the sensitivity of your model through innovative methods"
    def scaleText = "Scale up your experimentations with no effort on servers, clusters, grids, clouds, ..."

    def desktop = {
      def colTag: Seq[Modifier] = Seq(classIs("col-md-4"), attr("align") := "center")
      def rowTag: Seq[Modifier] = Seq(classIs(row))

      div(classIs(container_fluid), paddingTop := 40, paddingBottom := 40)(
        div(classIs(row + " centered-form center-block"))(
          div(rowTag, paddingBottom := 5)(
            div(colTag)(img(src := Resource.img.model.code.file, width := 120)),
            div(colTag)(img(src := Resource.img.method.exploreMap.file, width := 120)),
            div(colTag)(img(src := Resource.img.environment.scale.file, width := 120, paddingBottom := 10))
          ),
          div(rowTag)(
            div(colTag, mainTitle)("RUN"),
            div(colTag, mainTitle)("EXPLORE"),
            div(colTag, mainTitle)("SCALE")
          ),
          div(rowTag, paddingBottom := 5)(
            div(colTag)(div(mainText, width := 220, runText)),
            div(colTag)(div(mainText, width := 220, exploreText)),
            div(colTag)(div(mainText, width := 220, scaleText))
          ),
          div(rowTag)(
            div(colTag)(readMore(DocumentationPages.model)),
            div(colTag)(readMore(DocumentationPages.method)),
            div(colTag)(readMore(DocumentationPages.environment))
          )
        )
      )

    }

    def mobile = {
      def colTag: Seq[Modifier] = Seq(classIs("col"), attr("align") := "center")
      def rowTag: Seq[Modifier] = Seq(classIs(row))

      div(classIs(container_fluid), paddingTop := 50, paddingBottom := 20)(
        div(classIs(row + " centered-form center-block"))(
          div(rowTag)(
            div(colTag, paddingBottom := 30)(
              div(rowTag)(img(src := Resource.img.model.code.file, width := 120)),
              div(rowTag, mainTitle)("RUN"),
              div(rowTag)(div(mainText, width := 200, runText)),
              div(rowTag)(readMore(DocumentationPages.model))
            ),
            div(colTag, paddingBottom := 30)(
              div(rowTag)(img(src := Resource.img.method.exploreMap.file, width := 120)),
              div(rowTag, mainTitle)("EXPLORE"),
              div(rowTag)(div(mainText, width := 200, exploreText)),
              div(rowTag)(readMore(DocumentationPages.method))
            ),
            div(colTag)(
              div(rowTag)(img(src := Resource.img.environment.scale.file, width := 120)),
              div(rowTag, mainTitle)("SCALE"),
              div(rowTag)(div(mainText, width := 200, scaleText)),
              div(rowTag)(readMore(DocumentationPages.environment))
            )
          )
        )
      )
    }

    div(
      div(classIs("visible-lg visible-md"))(desktop),
      div(classIs("hidden-lg hidden-md"))(mobile)
    )

  }
}
