/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.replication

import scala.slick.driver.H2Driver.simple._

class Replicas(tag: Tag) extends Table[Replica](tag, "REPLICAS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def source = column[String]("SOURCE", O.NotNull)
  def storage = column[String]("STORAGE", O.NotNull)
  def path = column[String]("PATH", O.NotNull)
  def hash = column[String]("HASH", O.NotNull)
  def lastCheckExists = column[Long]("LAST_CHECK_EXISTS", O.NotNull)

  def idx1 = index("idx1", (source, hash, storage) /*, unique = true*/ )
  def idx2 = index("idx2", (source, storage))
  def idx3 = index("idx3", (storage))

  def * = (id, source, storage, path, hash, lastCheckExists) <> (Replica.tupled, Replica.unapply)
}

