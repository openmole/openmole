/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.rest.server.db

import java.sql.Blob

import scala.slick.driver.H2Driver.simple._

class ExecutionData(tag: Tag) extends Table[(Long, Blob, Blob)](tag, "ExecutionData") {
  def id = column[Long]("Id", O.PrimaryKey, O.AutoInc)
  def archive = column[Blob]("Archive", O.NotNull)
  def script = column[Blob]("Script", O.NotNull)
  def state = column[String]("State", O.NotNull)
  def * = (id, archive, script)
}
