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

/*package org.openmole.core.db

import slick.jdbc.H2Profile.api._

class Replicas(tag: Tag) extends Table[Replica](tag, "REPLICAS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def source = column[String]("SOURCE")
  def storage = column[String]("STORAGE")
  def path = column[String]("PATH")
  def hash = column[String]("HASH")
  def lastCheckExists = column[Long]("LAST_CHECK_EXISTS")

  def idx1 = index("idx1", (source, hash, storage))
  def idx2 = index("idx2", (path, storage))
  def idx3 = index("idx3", (hash, storage))

  def * = (id, source, storage, path, hash, lastCheckExists).mapTo[Replica] // <> (Tuple.fromProductTyped[Replica], summon[scala.deriving.Mirror.Of[Replica]].fromProduct)
}*/

