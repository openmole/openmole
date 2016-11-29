package org.openmole.gui.client.tool

/*
 * Copyright (C) 24/09/14 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.scalajs.dom._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.timers._
import scala.util.{ Failure, Success }

object OMPost extends autowire.Client[String, upickle.default.Reader, upickle.default.Writer] {
  override def doCall(req: Request): Future[String] = {
    val url = req.path.mkString("/")

    val timeout = setTimeout(10000) {
      println("The request is very long. Please check your connection.")
    }

    val future = ext.Ajax.post(
      url = s"$url",
      data = upickle.default.write(req.args),
      timeout = 60000
    ).map {
        _.responseText
      }

    future.onComplete {
      case Failure(t) ⇒ println(s"The request ${req.path.last} failed.")
      case Success(_) ⇒ clearTimeout(timeout)
    }

    future
  }

  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}