/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.tools

import java.time.{Duration => JDuration}
import java.util.UUID

import org.openmole.tool.random.Random

import concurrent.duration._
import scala.concurrent.duration.Duration

package object service {

  @transient lazy val localRuntimeInfo =
    RuntimeInfo(LocalHostName.localHostName.toOption.getOrElse("fake:" + UUID.randomUUID().toString))

  case class RuntimeInfo(hostName: String)

  def newRNG(seed: Long) = Random(seed)

  def stringToDuration(s: String): FiniteDuration = JDuration.parse(s).getSeconds seconds
  def stringFromDuration(d: Duration): String = JDuration.ofMillis(d.toMillis).toString

  implicit class StringDurationDecorator(s: String) {
    def toDuration = stringToDuration(s)
  }
}
