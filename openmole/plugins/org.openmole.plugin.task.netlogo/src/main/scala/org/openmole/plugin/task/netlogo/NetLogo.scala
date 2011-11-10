/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.task.netlogo

trait NetLogo {
  @throws(classOf[Exception])
  def open(script: String)
  
  @throws(classOf[Exception])
  def command(cmd: String)
  
  @throws(classOf[Exception])
  def report(variable: String): Any
  
  @throws(classOf[Exception])
  def dispose
  
  @throws(classOf[Exception])
  def globals : Array[String]
  
}
