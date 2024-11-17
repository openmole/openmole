package org.openmole.core.format

import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.context.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.{timeservice, workspace}
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.TmpDirectory
import org.scalatest.*
import io.circe.*
import org.openmole.tool.file.*

/*
 * Copyright (C) 2024 Romain Reuillon
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

object OMRSpec:
  enum Test:
    case T1, T2

class OMRSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  given TimeService = TimeService.stub()
  given Preference = Preference.stub()
  given FileService = FileService.stub()
  given tmpDirectory: TmpDirectory = TmpDirectory.stub()

  "OMR" should "support enum" in:
    val p = Val[String]
    val t = Val[OMRSpec.Test]
    val file = tmpDirectory.newFile("test", ".omr")

    val vs = Seq[Variable[?]](p -> "test", t -> OMRSpec.Test.T1)
    val data = OutputFormat.OutputContent(OutputFormat.SectionContent(Some("test"), vs))

    OMRFormat.write(
      data = data,
      methodFile = file,
      executionId = "test",
      jobId = 0,
      methodJson = Json.Null,
      script = None,
      timeStart = 1000,
      openMOLEVersion = "test",
      option = OMROption()
    )

    OMRFormat.variables(file).head._2 should equal(vs)

  "OMR indexes" should "work" in :
    val p = Val[Int]
    val file = tmpDirectory.newFile("test", ".omr")

    def writeData(v: Int) =
      val vs = Seq[Variable[?]](p -> v)
      val data = OutputFormat.OutputContent(OutputFormat.SectionContent(Some("test"), vs, indexes = Seq(p.name)))

      OMRFormat.write(
        data = data,
        methodFile = file,
        executionId = "test",
        jobId = 0,
        methodJson = Json.Null,
        script = None,
        timeStart = 1000,
        openMOLEVersion = "test",
        option = OMROption()
      )

    writeData(0)
    writeData(1)

    assert(OMRFormat.indexes(file).head.values.toSeq == Seq(0, 1))


  "OMR" should "store file" in:
    val p = Val[File]
    val file = tmpDirectory.newFile("test", ".omr")
    val storedFile = tmpDirectory.newFile("stored", ".txt")

    storedFile.content = "indeed\n"

    val vs = Seq[Variable[?]](p -> storedFile)
    val data = OutputFormat.OutputContent(OutputFormat.SectionContent(Some("test"), vs))

    OMRFormat.write(
      data = data,
      methodFile = file,
      executionId = "test",
      jobId = 0,
      methodJson = Json.Null,
      script = None,
      timeStart = 1000,
      openMOLEVersion = "test",
      option = OMROption()
    )

    OMRFormat.variables(file).head._2.head.value.asInstanceOf[File].content should equal(storedFile.content)