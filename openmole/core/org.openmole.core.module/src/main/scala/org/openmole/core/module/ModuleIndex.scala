/**
 * Created by Romain Reuillon on 12/09/16.
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
 *
 */
package org.openmole.core.module

import org.openmole.core.buildinfo
import org.openmole.core.preference.PreferenceLocation

object ModuleIndex {
  val moduleIndexes = PreferenceLocation("Module", "Indexes", Some(Seq[String](buildinfo.moduleAddress)))
}

case class Component(name: String, hash: String)
case class Module(name: String, description: String, components: Seq[Component])