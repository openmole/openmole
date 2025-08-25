package org.openmole.gui.server.ext

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

import cats.effect.IO
import org.openmole.gui.shared.data.*
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter

extension [A, B](e: sttp.tapir.Endpoint[Unit, A, ErrorData, B, Any])
  def implementedBy(f: A => B) =
    e.serverLogic: a =>
      IO.pure:
        try Right(f(a))
        catch
          case t: Throwable =>
            Left(ErrorData(t))

def routesFromEndpoints(r: ServerEndpoint[Fs2Streams[IO], IO]*) = ServerInterpreter().toRoutes(r.toList)
def ServerInterpreter() = Http4sServerInterpreter[IO]()

