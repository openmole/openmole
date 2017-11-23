package org.openmole.gui.client.core

/*
 * Copyright (C) 04/07/15 // mathieu.leclaire@openmole.org
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

import scala.concurrent.Future
import autowire._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.OMSettings
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._

object Settings {

  val settings: Future[OMSettings] = post()[Api].settings().call()
}
