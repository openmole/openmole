///*
// * Copyright (C) 2011 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package org.openmole.plugin.sampling.csv
//
//import java.io.{ File, FileReader }
//
//import au.com.bytecode.opencsv.CSVReader
//import org.openmole.core.context.{ Context, Val }
//import org.openmole.core.exception.UserBadDataError
//import org.openmole.core.dsl._
//import org.scalatest._
//
//import scala.util.Random
//
//class CSVSamplingSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {
//
//  "CSVSampling" should "detect the correct mapping between csv header defined column" in {
//    implicit val rng = new Random(42)
//
//    val p1 = Val[String]("col1")
//    val p2 = Val[Int]("col2")
//
//    val tmpCsvFile = File.createTempFile("tmp", ".csv")
//    getClass.getClassLoader.getResourceAsStream("csvTest.csv").copy(tmpCsvFile)
//    val reader = new CSVReader(new FileReader(tmpCsvFile))
//
//    val sampling =
//      CSVSampling(tmpCsvFile) set (
//        outputs += p1.mapped
//      )
//
//    sampling().from(Context.empty).toIterable.head.head.value should equal("first")
//
//    val sampling1 =
//      CSVSampling(tmpCsvFile) set (outputs += p2.mapped("badName"))
//
//    val exception = evaluating { sampling.toSampling.build(Context.empty) } should produce[UserBadDataError]
//  }
//}
