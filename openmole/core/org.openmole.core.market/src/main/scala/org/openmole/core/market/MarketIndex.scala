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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.market

import org.openmole.core.buildinfo
import org.openmole.core.preference.PreferenceLocation
import org.openmole.core.workspace.Workspace


case class MarketIndexEntry(name: String, archive: String, readme: Option[String], tags: Seq[String]):
  def url: String = org.openmole.core.buildinfo.marketURL(archive)

object MarketIndex:
  def empty = MarketIndex(Seq.empty)

case class MarketIndex(entries: Seq[MarketIndexEntry])
