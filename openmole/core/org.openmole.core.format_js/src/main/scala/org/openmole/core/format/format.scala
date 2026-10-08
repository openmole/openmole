package org.openmole.core.format

/*
 * Copyright (C) 2026 Romain Reuillon
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


import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

implicit val omrCirceDefault: io.circe.derivation.Configuration =
  io.circe.derivation.Configuration.default.withKebabCaseMemberNames.withDefaults.withDiscriminator("type").withTransformConstructorNames(derivation.renaming.kebabCase)
