/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.core.db

import org.scalatest._

import scala.util.Random
import squants.time.TimeConversions.*

class DBSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "File DB" should "be usable" in {
    val f = java.nio.file.Files.createTempFile("file", ".db")
    f.toFile.delete

    val t = databaseServer(f.toFile, 1 minutes)

    insert(t, "source", "storage", "path", "hash", 42) should matchPattern { case Transactor.Inserted(_) =>  }
    insert(t, "source", "storage", "path", "hash", 42) should matchPattern { case Transactor.AlreadyInDb(_) =>  }
    
    t.selectAll.size should equal(1)
    t.selectOnStorage("storage").size should equal(1)
    t.selectOnStorage("nestorage").size should equal(0)
    t.selectHash("hash").size should equal(1)
    t.selectPath("path").size should equal(1)
    t.selectSameSourceWithDifferentHash("source", "storage", "hh").size should equal(1)
    t.selectSameSourceWithDifferentHash("source", "storage", "hash").size should equal(0)
    t.selectSameSource("source", "storage", "hash").size should equal(1)
    t.selectPathsStorages(Seq("path"), Seq("storage")).size should equal(1)
    t.selectHashesStorages(Seq("hash"), Seq("storage")).size should equal(1)
    t.updateLastCheckExists(1, 43)
    t.selectSameSource("source", "storage", "hash").head.lastCheckExists should equal(43)
    t.close()

    val t2 = databaseServer(f.toFile, 1 minutes)
    t2.selectAll.size should equal(1)
    t2.delete(1)
    t2.selectAll.size should equal(0)
    t2.close()

  }

}