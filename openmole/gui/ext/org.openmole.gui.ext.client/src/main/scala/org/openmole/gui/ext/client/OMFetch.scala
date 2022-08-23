package org.openmole.gui.ext.client


/*
 * Copyright (C) 2022 Romain Reuillon
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

import endpoints4s.xhr.EndpointsSettings

import scala.concurrent.duration.*
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object OMFetch {
  def apply[API](api: EndpointsSettings => API) = new OMFetch(api)
}

class OMFetch[API](api: EndpointsSettings => API) {

  def future[O](
    f: API => scala.concurrent.Future[O],
    timeout: FiniteDuration = 60 seconds,
    warningTimeout: FiniteDuration = 10 seconds,
    onTimeout: () ⇒ Unit = () ⇒ {},
    onWarningTimeout: () ⇒ Unit = () ⇒ {},
    onFailed: Throwable => Unit = _ => {}) = {

    //TODO implement warning timeout
    //TODO add timoeut in settings
    val future = f(api(EndpointsSettings()))
    future.onComplete {
      case Failure(_: scala.concurrent.TimeoutException) ⇒ onTimeout()
      case Failure(t) => onFailed(t)
      case _ =>
    }

    future
  }

  def apply[O, R](
    r: API => scala.concurrent.Future[O],
    timeout: FiniteDuration = 60 seconds,
    warningTimeout: FiniteDuration = 10 seconds,
    onTimeout: () ⇒ Unit = () ⇒ {},
    onWarningTimeout: () ⇒ Unit = () ⇒ {},
    onFailed: Throwable => Unit = _ => {})(action: O => R) = {
    //import scala.concurrent.ExecutionContext.Implicits.global
    //    org.openmole.gui.client.core.APIClient.uuid(()).future.onComplete { i => println("uuid " + i.get.uuid) }
    val f = future(
      f = r,
      timeout = timeout,
      warningTimeout = warningTimeout,
      onTimeout = onTimeout,
      onWarningTimeout = onWarningTimeout,
      onFailed = onFailed)

    f.onComplete {
      case Success(r) ⇒ action(r)
      case _ =>
    }
  }


}