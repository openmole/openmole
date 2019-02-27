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

import org.openmole.core.context.{ Context, Val, PrototypeSet }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.DefaultSet
import org.scalatest._

class CopyFileHookSpec extends FlatSpec with Matchers {

  "A copy file misc" should "copy a file after the execution of a capsule" in {
    val f = File.createTempFile("test", ".tmp")

    val fw = new FileWriter(f)
    try fw.write("File contents!")
    finally fw.close

    val p = Val[File]("p")

    val t1 = new Task {
      val name = "Test"
      val outputs = PrototypeSet(p)
      val inputs = PrototypeSet.empty
      val plugins = PluginSet.empty
      val defaults = DefaultSet.empty
      override def process(context: Context) = context + (p → f)
    }

    val t1c = new MoleCapsule(t1)

    val fDest = File.createTempFile("test", ".tmp")

    val hook = CopyFileHook(p, fDest.getAbsolutePath)

    val ex = MoleExecution(Mole(t1c), hooks = List(t1c → hook))

    ex.start.waitUntilEnded

    f.hash should equal(fDest.hash)
    f.delete
    fDest.delete
  }

}
