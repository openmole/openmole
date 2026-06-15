package org.openmole.core.format

import io.circe.*
import org.openmole.core.context.*
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.TmpDirectory
import org.openmole.core.{timeservice, workspace}
import org.openmole.tool.file.*
import org.scalatest.*

import java.util.concurrent.atomic.AtomicInteger

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

import org.openmole.tool.types.TypeTool.iArrayManifest

class ValDataSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "ValData" should "build correct Val" in:
      def test(v: Val[?]) =
        val vd = ValData(v)
        ValData.toVal(vd) shouldEqual v

      test(Val[Int]("v"))
      test(Val[Array[Int]]("v"))
      test(Val[Map[Int, Array[Double]]]("v"))
      test(Val[IArray[Int]]("t"))

      type T = Array[Int]
      test(Val[T]("t"))


