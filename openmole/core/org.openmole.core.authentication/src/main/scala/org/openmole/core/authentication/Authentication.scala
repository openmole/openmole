/*
 * Copyright (C) 23/09/13 Romain Reuillon
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

package org.openmole.core.authentication

import java.io.File
import java.util.UUID

import org.openmole.core.workspace.Workspace
import org.openmole.tool.file.*
import org.openmole.tool.logger.JavaLogger
import io.circe.*

import scala.util.{ Failure, Success, Try }

object Authentication extends JavaLogger:
  def category[T](using m: Manifest[T]): String = s"${m.runtimeClass.getCanonicalName}.json"

  def save[T: {Manifest, Encoder, Decoder}](t: T, eq: (T, T) => Boolean)(using authenticationStore: AuthenticationStore) =
    authenticationStore.modify[T](category, s => s.filterNot(eq(_, t)) ++ Seq(t))

  def load[T: {Manifest, Decoder}](using authenticationStore: AuthenticationStore) =
    authenticationStore.load[T](category)

  def remove[T: {Manifest, Encoder, Decoder}](t: T, eq: (T, T) => Boolean)(using authenticationStore: AuthenticationStore) =
    authenticationStore.modify[T](category, s => s.filterNot(eq(_, t)))

  def clear[T](using m: Manifest[T], authenticationStore: AuthenticationStore): Unit =
     authenticationStore.clear(category[T])

