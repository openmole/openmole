package org.openmole.gui.shared.api

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

trait RESTAPI extends endpoints4s.algebra.Endpoints with endpoints4s.algebra.circe.JsonEntitiesFromCodecs with endpoints4s.circe.JsonSchemas:
   export io.circe.generic.auto.*
   type ErrorEndpoint[I, O] = Endpoint[I, Either[ErrorData, O]]
   def errorEndpoint[A, B](request: Request[A], r: Response[B], docs: EndpointDocs = EndpointDocs()) =
     endpoint(request, response(InternalServerError, jsonResponse[ErrorData]).orElse(r), docs)


