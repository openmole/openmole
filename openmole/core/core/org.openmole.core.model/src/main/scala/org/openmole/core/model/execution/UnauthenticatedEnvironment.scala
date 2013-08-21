/*
 * Copyright (C) 19/08/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.execution

object UnauthenticatedEnvironment {
  def apply[T <: Environment](f: AuthenticationProvider ⇒ T) =
    new UnauthenticatedEnvironment {
      type ENVIRONMENT = T
      def apply(authentications: AuthenticationProvider) = f(authentications)
    }
}

trait UnauthenticatedEnvironment {
  type ENVIRONMENT <: Environment
  def apply(authentications: AuthenticationProvider): ENVIRONMENT
}
