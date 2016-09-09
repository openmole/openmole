/**
 * Created by Romain Reuillon on 13/06/16.
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
package org.openmole.plugin.environment.batch.environment

import scala.concurrent.duration._

object UpdateInterval {
  def fixed(duration: FiniteDuration) = UpdateInterval(duration, duration, 0 second)
}

case class UpdateInterval(
  minUpdateInterval:       FiniteDuration,
  maxUpdateInterval:       FiniteDuration,
  incrementUpdateInterval: FiniteDuration
)
