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

import java.io.{ FileOutputStream, OutputStreamWriter }

import toolxit.bibtex._
import toolxit.bibtex.{ Number ⇒ BibTexNumber }

import scalatags.Text.all._

object Publication {

  def print(publication: BibtexEntry): Frag = {

    def authors = publication.get("Author")
    def title = publication.get("Title")
    def url = publication.get("Url")
    def journal = publication.get("Journal")
    def year = publication.get("Year")

    // and add a link to the file on the website
    Seq[Frag](authors, ", ", a(i(title), href := url), " published in ", journal, ", ", year)
  }

  def generateBibtex(publication: BibtexEntry): Frag = {

    // write bibtex to a separate file
    val bibfile = s"${publication.sortKey}.bib"
    val bibwriter = new OutputStreamWriter(new FileOutputStream(bibfile), "UTF8")

    bibwriter.write(publication.toBibTeX)
    bibwriter.flush
    bibwriter.close

    Seq[Frag](a(i("BibTex"), href := bibfile))
  }
}

object Papers {

  def fgcs2013 =
    Article(
      "Reuillon.etal.2013",
      Authors("Romain Reuillon", "Mathieu Leclaire", "Sebastien Rey-Coyrehourcq"),
      Title("OpenMOLE, a workflow engine specifically tailored for the distributed exploration of simulation models"),
      Journal("Future Generation Computer Systems"),
      Volume(29),
      BibTexNumber(8),
      Pages("1981 - 1990"),
      Year(2013),
      Url("http://www.openmole.org/files/FGCS2013.pdf")
    )
}
