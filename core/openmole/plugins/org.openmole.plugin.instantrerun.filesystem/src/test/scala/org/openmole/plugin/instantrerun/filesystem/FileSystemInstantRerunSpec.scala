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

package org.openmole.plugin.instantrerun.filesystem

import org.openmole.misc.workspace._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import java.io.File

import org.openmole.misc.tools.io.FileUtil._
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class FileSystemInstantRerunSpec extends FlatSpec with ShouldMatchers {

  "The instant rerun" should "avoid the second execution of the task" in {
    val p = Prototype[Long]("p")
    val res = new ListBuffer[Long]

    val t1 = new Task {
      val name = "Test instant rerun"
      val inputs = DataSet.empty
      val outputs = DataSet(p)
      val parameters = ParameterSet.empty
      val plugins = PluginSet.empty
      override def process(context: Context) = Context.empty + (p -> System.currentTimeMillis)
    }

    val t2 = new Task {
      val name = "Add result"

      val inputs = DataSet(p)
      val outputs = DataSet.empty
      val parameters = ParameterSet.empty
      val plugins = PluginSet.empty

      override def process(context: Context) = {
        res += context.value(p).get
        context
      }
    }

    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)

    val mole = t1c -- t2c

    val dir = File.createTempFile("testInstantRerun", "")
    dir.delete
    dir.mkdir

    new MoleExecution(mole, rerun = new FileSystemInstantRerun(dir, t1c)).start.waitUntilEnded
    new MoleExecution(mole, rerun = new FileSystemInstantRerun(dir, t1c)).start.waitUntilEnded
    dir.recursiveDelete

    res.distinct.size should equal(1)
  }

}
