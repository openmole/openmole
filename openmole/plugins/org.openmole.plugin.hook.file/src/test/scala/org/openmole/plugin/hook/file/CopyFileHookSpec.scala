/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.hook.file

import java.io.{ File, FileWriter }

import org.openmole.core.dsl._
import org.openmole.core.workflow.test.TestTask
import org.openmole.tool.hash._
import org.scalatest._

class CopyFileHookSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "A copy file misc" should "copy a file after the execution of a capsule" in {
    val f = File.createTempFile("test", ".tmp")

    val fw = new FileWriter(f)
    try fw.write("File contents!")
    finally fw.close

    val p = Val[File]("p")

    val t1 = TestTask { context => context + (p → f) } set (outputs += p)

    val fDest = File.createTempFile("test", ".tmp")

    val hook = CopyFileHook(p, fDest.getAbsolutePath)

    val ex = t1 hook hook

    ex.run()

    f.hash() should equal(fDest.hash())
    f.delete
    fDest.delete
  }

}
