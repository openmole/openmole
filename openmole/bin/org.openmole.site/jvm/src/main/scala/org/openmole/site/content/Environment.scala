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

package org.openmole.site.content

import org.openmole.site.tools._

import scalatags.Text.all._

object Environment:

  def provideOptions = """You also can set options by providing additional parameters to the environment (..., option = value, ...)"""
  def wallTime(name: String = "wallTime") = newEntry(name = name, " the maximum time a job is permitted to run before being killed, for instance ", hl.openmoleNoTest(s"$name = 1 hour"))
  def memory = newEntry("memory", " the memory for the job, for instance ", hl.openmoleNoTest("memory = 2 gigabytes"))
  def openMOLEMemory = newEntry("openMOLEMemory", " the memory of attributed to the OpenMOLE runtime on the execution node, if you run external tasks you can reduce the memory for the OpenMOLE runtime to 256MB in order to have more memory for you program on the execution node, for instance ", hl.openmoleNoTest("openMOLEMemory = 256 megabytes"))
  def threads = newEntry("threads", " the number of threads for concurrent execution of tasks on the worker node, for instance ", hl.openmoleNoTest("threads = 4"))
  def queue(name: String = "queue") = newEntry(name, " the name of the queue on which jobs will be submitted, for instance ", hl.openmoleNoTest(s"""$name = \"longjobs\""""))
  def port = newEntry("port", " the port number used by the ssh server, by ", b("default it is set to 22"))
  def sharedDirectory = newEntry("sharedDirectory", " OpenMOLE uses this directory to communicate from the head node of the cluster to the worker nodes (defaults to ", hl.openmoleNoTest("\"/home/user/.openmole/.tmp/ssh\")"))
  def storageSharedLocally = newEntry("storageSharedLocally", " When set to ", hl.openmoleNoTest("true"), ", OpenMOLE will use symbolic links instead of physically copying files to the remote environment. This ", b("assumes that the OpenMOLE instance has access to the same storage space as the remote environment"), " (think same NFS filesystem on desktop machine and cluster). Defaults to ", hl.openmoleNoTest("false"), " and shouldn't be used unless you're 100% sure of what you're doing!")
  def workDirectory = newEntry("workDirectory", " the directory in which OpenMOLE will execute on the remote server, for instance ", hl.openmoleNoTest("workDirectory = \"/tmp\""), "(defaults to ", hl.openmoleNoTest("\"/tmp\")"))
  def localSubmission = newEntry("localSubmission", " set to true if you are running OpenMOLE from a node of the cluster (useful for example if you have a cluster that you can only ssh behind a VPN but you can not set up the VPN where your OpenMOLE is running); user and host are not mandatory in this case")
  def modules = newEntry("modules", "a sequence of String to load modules on the execution environment using \"module load name\", for instance ", hl.openmoleNoTest(s"""modules = Seq("singularity")"""))

  def apiEntryTitle(entryName: String): Frag = Seq[Frag](b(entryName), ": ")
  def newEntry(name: String, body: Frag*): Frag = Seq[Frag](apiEntryTitle(name), body)
