/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.site
import scalatags.Text.all._

object Environment {

  def envOption(option: String): Frag = Seq[Frag](b(option), ": ")

  def provideOptions = """You also can set options by providing additional parameters to the environment (..., option = value, ...)"""
  def wallTime: Frag = Seq[Frag](envOption("walltime"), " the maximum duration for the job in term of user time, for instance ", hl.openmoleNoTest("wallTime = 1 hour"))
  def memory: Frag = Seq[Frag](envOption("memory"), " the memory in mega-byte for the job, for instance ", hl.openmoleNoTest("memory = 2000"))
  def openMOLEMemory: Frag = Seq[Frag](envOption("openMOLEMemory"), " the memory of attributed to the OpenMOLE runtime on the execution node, if you run external tasks you can reduce the memory for the OpenMOLE runtime to 256MB in order to have more memory for you program on the execution node, for instance ", hl.openmoleNoTest("openMOLEMemory = 256"))
  def threads: Frag = Seq[Frag](envOption("threads"), " the number of threads for concurrent execution of tasks on the worker node, for instance ", hl.openmoleNoTest("threads = 4"))
  def queue: Frag = Seq[Frag](envOption("queue"), " the name of the queue on which jobs should be submitted, for instance ", hl.openmoleNoTest("queue = \"longjobs\""))
  def port: Frag = Seq[Frag](envOption("port"), " the number of the port used by the ssh server, by ", b("default it is set to 22"))
  def sharedDirectory: Frag = Seq[Frag](envOption("sharedDirectory"), " OpenMOLE uses this directory to communicate from the head of the cluster to the worker nodes (", hl.openmoleNoTest("sharedDirectory = \"/home/user/openmole\""))
  def workDirectory: Frag = Seq[Frag](envOption("workDirectory"), " the directory in which OpenMOLE will run on the remote server, for instance ", hl.openmoleNoTest("workDirectory = \"${TMP}\""))
}
