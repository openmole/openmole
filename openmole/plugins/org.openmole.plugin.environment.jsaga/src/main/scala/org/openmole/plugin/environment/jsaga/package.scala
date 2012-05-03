/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment

import org.ogf.saga.job.JobDescription

package object jsaga {
  implicit def tupleOfSringToRequirement(t: (String, Any)) = new Requirement(t._1, t._2.toString)

  val MEMORY = JobDescription.TOTALPHYSICALMEMORY
  val CPU_TIME = JobDescription.TOTALCPUTIME
  val CPU_COUNT = JobDescription.TOTALCPUCOUNT
  val CPU_ARCHITECTURE = JobDescription.CPUARCHITECTURE

  def memory(size: Int) = new Requirement(MEMORY, size.toString)
  def cpuTime(time: String) = new Requirement(CPU_TIME, time)
  def cpuCount(nb: Int) = new Requirement(CPU_COUNT, nb.toString)
  def cpuArchitecture(arch: String) = new Requirement(CPU_ARCHITECTURE, arch)

  val x86_64 = cpuArchitecture("x86_64")
}