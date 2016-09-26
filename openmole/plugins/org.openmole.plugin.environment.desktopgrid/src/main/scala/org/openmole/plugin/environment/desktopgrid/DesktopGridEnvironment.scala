/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.desktopgrid

import org.openmole.core.dsl._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice.JobService
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.thread._

object DesktopGridEnvironment {
  val timeStempsDirName = "timeStemps"
  val jobsDirName = "jobs"
  val tmpJobsDirName = "tmpjobs"
  val resultsDirName = "results"
  val tmpResultsDirName = "tmpresults"
  val timeStempSeparator = '@'

  def apply(
    port:           Int,
    openMOLEMemory: OptionalArgument[Int]    = None,
    threads:        OptionalArgument[Int]    = None,
    name:           OptionalArgument[String] = None
  ) =
    new DesktopGridEnvironment(
      port = port,
      openMOLEMemory = openMOLEMemory,
      threads = threads,
      name = name
    )
}

class DesktopGridEnvironment(
    val port:                    Int,
    override val openMOLEMemory: Option[Int],
    override val threads:        Option[Int],
    override val name:           Option[String]
) extends SimpleBatchEnvironment { env ⇒

  type SS = StorageService
  type JS = JobService

  lazy val service = DesktopGridService.borrow(port)

  override def finalize() = background { DesktopGridService.release(port) }
  override val storage = service.storage(this, port)
  override val jobService = service.jobService(this, port)
}
