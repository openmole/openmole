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

package org.openmole.plugin.sampling.csv

import au.com.bytecode.opencsv.CSVReader
import java.io.File
import java.io.FileReader
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.workflow.data._
import org.scalatest._
import FileUtil._

import scala.util.Random

class CSVSamplingSpec extends FlatSpec with Matchers {

  "CSVSampling" should "detect the correct mapping between csv header defined column" in {
    implicit val rng = new Random(42)

    val p1 = Prototype[String]("col1")
    val p2 = Prototype[Int]("col2")

    val tmpCsvFile = File.createTempFile("tmp", ".csv")
    getClass.getClassLoader.getResourceAsStream("csvTest.csv").copy(tmpCsvFile)
    val reader = new CSVReader(new FileReader(tmpCsvFile))
    val sampling = CSVSampling(tmpCsvFile)
    sampling addColumn p1

    sampling.build(Context.empty).toIterable.head.head.value should equal("first")

    sampling addColumn ("badName", p2)
    val exception = evaluating { sampling.toSampling.build(Context.empty) } should produce[UserBadDataError]
  }
}
