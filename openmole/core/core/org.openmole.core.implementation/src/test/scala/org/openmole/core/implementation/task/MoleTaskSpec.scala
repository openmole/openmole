/*
 * Copyright (C) 16/02/13 Romain Reuillon
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

package org.openmole.core.implementation.task

import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.scalatest.FlatSpec
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.openmole.core.implementation.mole._
import org.openmole.core.model.task.PluginSet

@RunWith(classOf[JUnitRunner])
class MoleTaskSpec extends FlatSpec with Matchers {

  "Implicits" should "work with mole task" in {
    val i = Prototype[String]("i")
    val emptyT = EmptyTask("Empty")
    emptyT.addInput(i)

    val emptyC = new Capsule(emptyT)

    val moleTask =
      MoleTask(
        "MoleTask",
        Mole(emptyC), emptyC)(PluginSet.empty)

    moleTask addImplicit i
    moleTask addParameter (i -> "test")

    MoleExecution(Mole(moleTask)).start.waitUntilEnded
  }

}
