package org.openmole.site.content.documentation

/*
 * Copyright (C) 2023 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Native._

object Documentation extends PageContent(

ul(listStyleType := "none")(
  li(
    h3(a(plug.title, href := plug.file)),
    ol(sitemap.siteMapSection(plugPages))
  ),
  li(
    h3(a(explore.title, href := explore.file)),
    ol(
      li(
        a(samplings.title, href := samplings.file),
        ol(listStyleType:= "lower-alpha")(
          sitemap.siteMapSection(samplingPages)
        )
      ),
      sitemap.siteMapSection(explorePages.diff(Seq(samplings)))
    )
  ),
  li(
    h3(a(scale.title, href := scale.file)),
    ol(sitemap.siteMapSection(scalePages))
  ),
  li(
    h3(a(utilityTask.title, href := utilityTask.file)),
    ol(sitemap.siteMapSection(utilityTaskPages))
  ),
  li(
    h3(a(language.title, href := language.file)),
    ol(sitemap.siteMapSection(languagePages))
  ),
  li(
    h3(a("Advanced Concepts", href := geneticAlgorithm.file)),
    ol(sitemap.siteMapSection(advancedConceptsPages))
  ),
  li(
    h3(a(developers.title, href := developers.file)),
    ol(sitemap.siteMapSection(developersPages))
  ),
  h3("See also"),
  sitemap.siteMapSection(docLonelyPages)
)

)
