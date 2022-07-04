package org.openmole.gui.ext.api

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

import endpoints4s.{algebra, generic}

object NewAPI {
  case class Test(uuid: String)
}

trait NewAPI extends algebra.Endpoints
  with algebra.JsonEntitiesFromSchemas
  with generic.JsonSchemas {

  val uuid: Endpoint[Unit, NewAPI.Test] =
    endpoint(get(path / "uuid"), ok(jsonResponse[NewAPI.Test]))


  implicit lazy val fooSchema: JsonSchema[NewAPI.Test] = genericJsonSchema

}
