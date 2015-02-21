/*
 * Copyright (C) 2015 Romain Reuillon
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
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
package org.openmole.site
package credits

import toolxit.bibtex._

import scalatags.Text.all._

object Publication {
  def fgcs2013 =
    Publication(
      Seq("Romain Reuillon", "Mathieu Leclaire", "Sebastien Rey-Coyrehourcq"),
      Title("OpenMOLE, a workflow engine specifically tailored for the distributed exploration of simulation models"),
      "Future Generation Computer Systems, vol 29, num 8, pp 1981-1990",
      2013,
      url = "http://www.openmole.org/files/FGCS2013.pdf"
    )

  def fgcs2013_B =
    Article(
      "Reuillon.etal.2013",
      Authors("Romain Reuillon", "Mathieu Leclaire", "Sebastien Rey-Coyrehourcq"),
      Title("OpenMOLE, a workflow engine specifically tailored for the distributed exploration of simulation models"),
      Journal("Future Generation Computer Systems, vol 29, num 8, pp 1981-1990"),
      Year(2013),
      Url("http://www.openmole.org/files/FGCS2013.pdf")
    )

  def publication(publication: Publication): Frag = {
    def authors = publication.authors.map(s ⇒ s: Frag).reduceLeft { (a1, a2) ⇒ Seq[Frag](a1, ", ", a2): Frag }
    Seq[Frag](authors, ", ", a(i(publication.title.value.toString), href := publication.url), " published in ", publication.in, ", ", publication.year)
  }

  def publication2(publication: BibtexEntry): Frag = {

    def authors = publication.get("Author")
    def title = publication.get("Title")
    def url = publication.get("Url")
    def journal = publication.get("Journal")
    def year = publication.get("Year")
    Seq[Frag](authors, ", ", a(i(title), href := url, " published in ", journal, ", ", year), "\n", publication.toBibTeX)
  }
}
case class Publication(authors: Seq[String], title: Title, in: String, year: Int, url: String)
