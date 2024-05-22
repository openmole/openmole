package org.openmole.site.content.documentation.utilityTask

/*
 * Copyright (C) 2023 Romain Reuillon
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

import org.openmole.site.content.header.*

object TryTask extends PageContent(html"""

A ${i{"TryTask"}} encapsulates a task. It is useful in case your task may fail and you want to provide some alternative output when it does.

$br

To encapsulate a task in a ${i{"TryTask"}}, simply wrap it in the ${i{"TryTask"}} builder:

$br

${hl.openmole("""
  val result = Val[Double]

  val t1 = ScalaTask("val result = someProcessThatMayFail()") set (
    outputs += result
  )

  TryTask(t1) set (
    result := 10.0 // Value of result in case t1 fails
  )
  """)}

$br

A ${i{"RetryTask"}} encapsulates a task. It is useful in case your task may fail and you want to retry to execute them multiple times.

$br

To encapsulate a task in a ${i{"RetryTask"}}, simply wrap it in the ${i{"RetryTask"}} builder:

$br

${hl.openmole("""
  val result = Val[Double]

  val t1 = ScalaTask("val result = someProcessThatMayFail()") set (
    outputs += result
  )

  RetryTask(t1, 5) // retry t1 5 times in case of failure
  """)}

""")
