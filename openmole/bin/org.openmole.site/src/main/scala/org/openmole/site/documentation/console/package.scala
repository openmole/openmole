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

package org.openmole.site.documentation.console

package object environment {
  def provideOptions = """You also can set options by providing additional parameters to the environment (..., option = value, ...): """
  def wallTime = """wallTime: the maximum duration for the job in term of user time, for instance wallTime = 1 hour"""
  def memory = """memory: the memory in mega-byte for the job, for instance memory = 2000"""
  def openMOLEMemory = """openMOLEMemory: the memory of attributed to the OpenMOLE runtime on the execution node, if you run external tasks you can reduce the memory for the OpenMOLE runtime to 256 mega in order to have more memory for you program on the execution node, for instance openMOLEMemory = 256"""
  def threads = """threads: the number of threads for concurrent execution of tasks on the worker node, for instance threads = 4"""
  def queue = """queue: the name of the queue on which jobs should be submitted, for instance queue = "longjobs""""
  def port = """port: the number of the port used by the ssh server, by default it is set to 22"""
  def workDirectory = """workDirectory: the directory in which OpenMOLE will run on the remote server, for instance workDirector = "/home/user/openmole/""""
}
