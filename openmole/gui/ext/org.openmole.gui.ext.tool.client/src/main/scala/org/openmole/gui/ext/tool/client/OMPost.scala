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

import boopickle.Default._
import java.nio.ByteBuffer

import org.scalajs.dom._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation._
import scala.scalajs.js.timers._
import scala.scalajs.js.typedarray.{ ArrayBuffer, TypedArrayBuffer }
import scala.util.{ Failure, Success }

@JSExportTopLevel("OMPost")
case class OMPost(
  timeout:              Duration        = 60 seconds,
  warningTimeout:       Duration        = 10 seconds,
  timeOutAction:        (String) ⇒ Unit = (s: String) ⇒ {},
  warningTimeoutAction: () ⇒ Unit       = () ⇒ {}
) extends autowire.Client[ByteBuffer, Pickler, Pickler] {

  override def doCall(req: Request): Future[ByteBuffer] = {
    val url = req.path.mkString("/")

    val timeoutSet = setTimeout(warningTimeout.toMillis) {
      warningTimeoutAction()
    }

    def stopTimeout = clearTimeout(timeoutSet)

    val future = ext.Ajax.post(
      url = s"$url",
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer",
      headers = Map("Content-Type" → "application/octet-stream"),
      timeout = timeout.toMillis.toInt
    ).map(r ⇒ TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))

    future.onComplete {
      case Failure(t) ⇒
        timeOutAction(req.path.last)
        stopTimeout
      case Success(_) ⇒ stopTimeout
    }

    future
  }

  override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

  override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
}