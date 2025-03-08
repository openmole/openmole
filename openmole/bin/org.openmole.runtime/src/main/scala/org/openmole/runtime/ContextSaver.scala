/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.runtime

import java.util.concurrent.Semaphore

import org.openmole.core.context._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.job.State._
import org.openmole.tool.logger.JavaLogger

import scala.collection.immutable.TreeMap
import util.{ Failure, Success, Try }

object ContextSaver extends JavaLogger

import ContextSaver.Log._

class ContextSaver(val nbJobs: Int) {

  val allFinished = new Semaphore(0)

  var nbFinished = 0
  var _results = new TreeMap[JobId, Try[Context]]
  def results = _results

  def save(job: JobId, result: Either[Context, Throwable]) = synchronized {
    result match {
      case Left(context) =>
        logger.fine(s"Job success ${job} ${context}")
        _results += job → Success(context)
      case Right(t) =>
        logger.log(FINE, s"Job failure ${job}", t)
        _results += job → Failure(t)
    }

    nbFinished += 1
    if (nbFinished >= nbJobs) allFinished.release
  }

  def waitAllFinished = {
    allFinished.acquire
    allFinished.release
  }

}
