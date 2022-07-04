package org.openmole.gui.server.newcore

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

import cats.effect._
import endpoints4s.http4s.server
import org.http4s._
import org.openmole.gui.ext.api

/** Defines a Play router (and reverse router) for the endpoints described
 * in the `CounterEndpoints` trait.
 */
object APIServer
  extends server.Endpoints[IO]
    with api.NewAPI
    with server.JsonEntitiesFromSchemas {

  /** Simple implementation of an in-memory counter */
  // val counter = Ref(0)

  // Implements the `currentValue` endpoint
  val uuidRoute =
    uuid.implementedBy(_ => api.NewAPI.Test(java.util.UUID.randomUUID().toString))

//  // Implements the `increment` endpoint
//  val fooRoute =
//    foo.implementedBy(_ => shared.Data.Foo(7))
//  //
//  //  val routes: Route =
//  //    uuidRoute ~ fooRoute

  //FIXME remove cors when on default port
  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(uuidRoute)
  ).map(_.putHeaders(Header("Access-Control-Allow-Origin", "*")))

}
