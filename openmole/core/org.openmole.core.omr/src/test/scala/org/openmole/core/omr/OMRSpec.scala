package org.openmole.core.omr

import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.context.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
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


class OMRSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "OMR" should "support enum" in:
    given TimeService = TimeService.stub()
    given Preference = Preference.stub()
    given FileService = FileService.stub()
    given tmpDirectory: TmpDirectory = TmpDirectory.stub()
    given SerializerService = SerializerService.stub()

    val p = Val[String]
    val file = tmpDirectory.newFile("test", ".omr")

    val data = Seq(OMRFormat.SectionData(Some("test"), Seq(p -> "test")))

    OMRFormat.write(
      data = data,
      methodFile = file,
      executionId = "test",
      jobId = 0,
      methodJson = Json.Null,
      script = None,
      timeStart = 1000,
      openMOLEVersion = "test",
      append = false,
      overwrite = true
    )

    OMRFormat.toVariables(file)
  

