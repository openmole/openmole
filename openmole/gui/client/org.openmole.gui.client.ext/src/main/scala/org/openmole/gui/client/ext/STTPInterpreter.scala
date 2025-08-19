package org.openmole.gui.client.ext

/*
 * Copyright (C) 2025 Romain Reuillon
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


import sttp.tapir.client.sttp4.*
import sttp.client4.*
import sttp.tapir.PublicEndpoint
import concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class STTPInterpreter:

  lazy val sttpInterpreter = SttpClientInterpreter()
  lazy val backend = DefaultFutureBackend()

  def toRequest[I, E, O](e: PublicEndpoint[I, E, O, Any])(using pb: BasePath)(i: I): Future[O] =
    val uri = pb.map(u => sttp.model.Uri.pathRelative(u.split("/").toSeq))
    sttpInterpreter.toRequestThrowErrors(e, uri)(i).send(backend).flatMap: r =>
      Future.successful(r.body)
