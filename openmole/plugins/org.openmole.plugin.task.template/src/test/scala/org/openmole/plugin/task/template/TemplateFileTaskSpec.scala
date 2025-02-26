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

package org.openmole.plugin.task.template

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import org.openmole.core.workflow.test.TestHook
import org.openmole.tool.hash._
import org.scalatest._

class TemplateFileTaskSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "A template file generator task" should "parse a template file and evalutate the ${} expressions" in {

    lazy val templateFile: File = {
      val template = java.io.File.createTempFile("file", ".test")
      template.content =
        """My first line
          |${2*3}
          |I am ${a*5} year old
          |${s"I am ${a*5} year old"}""".stripMargin
      template
    }

    lazy val targetFile: File = {
      val target = java.io.File.createTempFile("target", ".test")
      target.content =
        """My first line
          |6
          |I am 30 year old
          |I am 30 year old""".stripMargin
      target
    }

    val outputP = Val[File]
    val a = Val[Int]

    val t1 = TemplateFileTask(templateFile, outputP) set (a := 6)
    val testHook = TestHook { c => targetFile.hash() should equal(c(outputP).hash()) }
    (t1 hook testHook).run()
  }

}