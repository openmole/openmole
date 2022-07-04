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

import endpoints4s.xhr
import endpoints4s.xhr.EndpointsSettings
import org.openmole.gui.ext.api

/** Defines an HTTP client for the endpoints described in the `CounterEndpoints` trait.
 * The derived HTTP client uses XMLHttpRequest to perform requests and returns
 * results in a `js.Thenable`.
 */
object APIClient
  extends api.NewAPI
    with xhr.future.Endpoints
    with xhr.JsonEntitiesFromSchemas {
  lazy val settings: EndpointsSettings = EndpointsSettings().withBaseUri(Some("http://localhost:46858"))
}