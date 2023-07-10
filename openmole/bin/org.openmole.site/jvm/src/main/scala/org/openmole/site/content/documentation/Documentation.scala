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

import org.openmole.site.content.header.*

object Documentation extends PageContent(

ul(listStyleType := "none")(
  li(
    h3(a(DocumentationPages.plug.title.get, href := DocumentationPages.plug.file)),
    ol(sitemap.siteMapSection(DocumentationPages.plugPages))
  ),
  li(
    h3(a(DocumentationPages.explore.title.get, href := DocumentationPages.explore.file)),
    ol(
      li(
        a(DocumentationPages.samplings.title.get, href := DocumentationPages.samplings.file),
        ol(listStyleType:= "lower-alpha")(
          sitemap.siteMapSection(DocumentationPages.samplingPages)
        )
      ),
      sitemap.siteMapSection(DocumentationPages.explorePages.diff(Seq(DocumentationPages.samplings)))
    )
  ),
  li(
    h3(a(DocumentationPages.scale.title.get, href := DocumentationPages.scale.file)),
    ol(sitemap.siteMapSection(DocumentationPages.scalePages))
  ),
  li(
    h3(a(DocumentationPages.utilityTask.title.get, href := DocumentationPages.utilityTask.file)),
    ol(sitemap.siteMapSection(DocumentationPages.utilityTaskPages))
  ),
  li(
    h3(a(DocumentationPages.language.title.get, href := DocumentationPages.language.file)),
    ol(sitemap.siteMapSection(DocumentationPages.languagePages))
  ),
  li(
    h3(a("Advanced Concepts", href := DocumentationPages.geneticAlgorithm.file)),
    ol(sitemap.siteMapSection(DocumentationPages.advancedConceptsPages))
  ),
  li(
    h3(a(DocumentationPages.developers.title.get, href := DocumentationPages.developers.file)),
    ol(sitemap.siteMapSection(DocumentationPages.developersPages))
  ),
  h3("See also"),
  sitemap.siteMapSection(DocumentationPages.docLonelyPages)
)

)
