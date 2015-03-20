/*
 * Copyright (C) 2015 Romain Reuillon
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

import scalatags.Text.all._

object Developer {

  def geocite = Affiliation("Géographie-citées", "http://parisgeo.cnrs.fr")
  def iscpif = Affiliation("Institut des Systèmes Complexes de Paris (ISC-PIF)", "http://www.iscpif.fr")
  def biomedia = Affiliation("Biomedical Image Analysis (BioMedIA)", "http://biomedic.doc.ic.ac.uk")

  def affiliation(affiliation: Affiliation): Frag = a(affiliation.name, href := affiliation.site)

  def active: Frag = {
    def all =
      Seq(
        Developer("Romain Reuillon", Seq(geocite, iscpif)),
        Developer("Mathieu Leclaire", Seq(geocite, iscpif)),
        Developer("Jonathan Passerat-Palmbach", Seq(biomedia))
      )

    def lines =
      for {
        dev ← all
      } yield {
        def affiliations =
          dev.affiliations.map(affiliation).reduce { (a1, a2) ⇒ Seq[Frag](a1, " and ", a2): Frag }
        Seq[Frag](i(dev.name), " from ", affiliations)
      }

    lines.map(l ⇒ Seq[Frag](l, br))
  }
}

case class Affiliation(name: String, site: String)
case class Developer(name: String, affiliations: Seq[Affiliation])
