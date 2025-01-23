package org.openmole.plugin.task.external

import org.scalatest.{flatspec, matchers}
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

/*
 * Copyright (C) 2025 Romain Reuillon
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

class ExternalTaskSpec  extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.*
  import Stubs.*

  "ExternalTask " should "run after serialization and deserialization" in :
    val x = Val[Double]

    val task =
      ExternalTask.build("Test"): b =>
        val i = 2
        ExternalTask.execution: e =>
          Context(x -> e.context(x) * i)
      .set (x := 2, outputs += x)

    val test =
      TestHook: c =>
        c(x) should equal(4)

    val t2 = serializeDeserialize(task)
    (t2 hook test).run()

