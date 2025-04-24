package org.openmole.gui.server.ext

import cats.effect.IO
import endpoints4s.http4s.server
import org.openmole.gui.shared.data.*

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

import org.http4s
import org.http4s.*

abstract class APIServer extends server.Endpoints[IO] with server.JsonEntitiesFromCodecs:

  implicit class EndpointDecorator[A, B](ep: Endpoint[A, Either[ErrorData, B]]):
    def errorImplementedBy(f: A => B) =
      ep.implementedBy: a =>
        try Right(f(a))
        catch
          case t: Throwable =>
            Left(ErrorData(t))


