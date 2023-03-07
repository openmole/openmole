package org.openmole.gui.client.core

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


import org.openmole.gui.client.ext.OMFetch

import scala.concurrent.duration.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*


class Fetch(panels: Panels):

  def future[O](
    f: CoreAPIClientImpl => Future[O],
    timeout: Option[FiniteDuration] = Some(60 seconds),
    warningTimeout: Option[FiniteDuration] = Some(10 seconds))(using path: BasePath) =

    OMFetch(coreAPIClient).future(
      f,
      timeout = timeout,
      warningTimeout = warningTimeout,
      onTimeout = () => panels.notifications.addAndShowNotificaton(NotificationLevel.Error, "The request timed out. Please check your connection."),
      onWarningTimeout = () => panels.notifications.addAndShowNotificaton(NotificationLevel.Info, "The request is very long. Please check your connection."),
      onFailed = t => panels.notifications.addAndShowNotificaton(NotificationLevel.Error, s"The request failed with error $t")
    )

  def futureError[O](
     f: CoreAPIClientImpl => Future[Either[ErrorData, O]],
     timeout: Option[FiniteDuration] = Some(60 seconds),
     warningTimeout: Option[FiniteDuration] = Some(10 seconds))(using path: BasePath) =

    OMFetch(coreAPIClient).futureError(
      f,
      timeout = timeout,
      warningTimeout = warningTimeout,
      onTimeout = () => panels.notifications.addAndShowNotificaton(NotificationLevel.Error, "The request timed out. Please check your connection."),
      onWarningTimeout = () => panels.notifications.addAndShowNotificaton(NotificationLevel.Info, "The request is very long. Please check your connection."),
      onFailed =
        case OMFetch.ServerError(e) =>
          panels.notifications.addAndShowNotificaton(NotificationLevel.Error,
            s"""The server returned an error 500:
               |${ErrorData.stackTrace(e)}""".stripMargin)
        case t =>
          panels.notifications.addAndShowNotificaton(NotificationLevel.Error,
            s"""The server failed unexpectedly:
               |${t}""".stripMargin)
    )
