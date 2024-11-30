package org.openmole.core.project

/*
 * Copyright (C) 2021 Romain Reuillon
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


import org.openmole.core.services.Services
import org.openmole.tool.file.*
import org.scalatest.{flatspec, matchers}

class ProjectSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:
  import org.openmole.core.workflow.test.*

  "Project compiler" should "complete code" in:
    val workDirectory = tmpDirectory.newDirectory("project")
    val script = workDirectory / "script.oms"


    Services.withServices(workDirectory) { implicit services =>
      val begin =
        """
          |def theFunction(i: Int) = i * 2
          |val k = theF""".stripMargin

      val f1 = "math.c"

      val f2 = """val i = EmptyT"""

      script.content =
        s"""$begin
          |$f1
          |$f2
          |val l = 2 * 2""".stripMargin

      Project.completion(workDirectory, script, begin.size).exists(_.label == "theFunction") should equal(true)
      Project.completion(workDirectory, script, begin.size + f1.size + 1).exists(_.label == "cos") should equal(true)
      Project.completion(workDirectory, script, begin.size + f1.size + f2.size + 2).exists(_.label == "EmptyTask") should equal(true)
    }




