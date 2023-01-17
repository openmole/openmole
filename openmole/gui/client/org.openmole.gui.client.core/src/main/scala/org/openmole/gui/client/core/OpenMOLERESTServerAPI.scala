package org.openmole.gui.client.core

/*
 * Copyright (C) 2022 Romain Reuillon
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

import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import scala.concurrent.Future

class OpenMOLERESTServerAPI(fetch: Fetch) extends ServerAPI:
  def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean) = fetch.future(_.copyFiles(safePaths, to, overwrite).future)
  def size(safePath: SafePath) = fetch.future(_.size(safePath).future)

class StubRestServerAPI extends ServerAPI:
  override def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean): Future[Seq[SafePath]] = Future.successful(Seq())
  override def size(safePath: SafePath): Future[Long] = Future.successful(0L)
