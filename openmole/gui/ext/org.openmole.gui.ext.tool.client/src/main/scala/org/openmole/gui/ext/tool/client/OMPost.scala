package org.openmole.gui.ext.tool.client

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
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.annotation._
import scala.scalajs.js.timers._
import scala.util.{ Failure, Success }

@JSExportTopLevel("OMPost")
case class OMPost(timeout: Duration = 60 seconds, warningTimeout: Duration = 10 seconds, timeOutAction: (String) ⇒ Unit = (s: String) ⇒ {}, warningTimeoutAction: () ⇒ Unit = () ⇒ {}) extends autowire.Client[String, upickle.default.Reader, upickle.default.Writer] {

  override def doCall(req: Request): Future[String] = {
    val url = req.path.mkString("/")

    val timeoutSet = setTimeout(warningTimeout.toMillis) {
      warningTimeoutAction()
    }

    def stopTimeout = clearTimeout(timeoutSet)

    val future = ext.Ajax.post(
      url = s"$url",
      data = upickle.default.write(req.args),
      timeout = timeout.toMillis.toInt
    ).map {
        _.responseText
      }

    future.onComplete {
      case Failure(t) ⇒
        timeOutAction(req.path.last)
        stopTimeout
      case Success(_) ⇒ stopTimeout
    }

    future
  }

  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}