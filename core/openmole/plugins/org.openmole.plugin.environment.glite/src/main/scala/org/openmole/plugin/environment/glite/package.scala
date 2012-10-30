/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.environment

import org.openmole.core.implementation.tools._

package object glite {

  lazy val complexsystems =
    new GliteEnvironment(
      "vo.complex-systems.eu",
      "voms://voms.grid.auth.gr:15160/C=GR/O=HellasGrid/OU=auth.gr/CN=voms.grid.auth.gr",
      "ldap://topbdii.grif.fr:2170")

  lazy val biomed =
    new GliteEnvironment(
      "biomed",
      "voms://cclcgvomsli01.in2p3.fr:15000/O=GRID-FR/C=FR/O=CNRS/OU=CC-IN2P3/CN=cclcgvomsli01.in2p3.fr",
      "ldap://topbdii.grif.fr:2170")

  lazy val francegrilles =
    new GliteEnvironment(
      "vo.france-grilles.fr",
      "voms://cclcgvomsli01.in2p3.fr:15017/O=GRID-FR/C=FR/O=CNRS/OU=CC-IN2P3/CN=cclcgvomsli01.in2p3.fr",
      "ldap://topbdii.grif.fr:2170")

}