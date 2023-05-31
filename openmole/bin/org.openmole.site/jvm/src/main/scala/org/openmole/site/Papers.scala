/*
 *  Copyright (C) 2017 Jonathan Passerat-Palmbach
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.site

object Papers {

  import scalatags.Text.all._

  lazy val frontiers2017 = tools.paragraph(
    "Jonathan Passerat-Palmbach, Romain Reuillon, Mathieu Leclaire, Antonios Makropoulos, Emma C. Robinson, Sarah Parisot and Daniel Rueckert, ",
    b("Reproducible Large-Scale Neuroimaging Studies with the OpenMOLE Workflow Management System"),
    ", published in ", i("Frontiers in Neuroinformatics"), " Vol 11, 2017.",
    br,
    a("[online version] ", href := "http://journal.frontiersin.org/article/10.3389/fninf.2017.00021/full#"),
    a("[bibteX]", href := Resource.bibtex.frontierBib.file)
  )

}
