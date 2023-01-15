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



import org.openmole.gui.client.core.alert.{BannerAlert, BannerLevel}

import scala.concurrent.duration.*
import org.openmole.gui.ext.client.OMFetch

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import org.openmole.gui.ext.client.{CoreAPIClientImpl, coreAPIClient}

class Fetch(alert: (String, BannerLevel) => Unit):

  def future[O](
    f: CoreAPIClientImpl => Future[O],
    timeout: FiniteDuration = 60 seconds,
    warningTimeout: FiniteDuration = 10 seconds) =

    OMFetch(coreAPIClient).future(
      f,
      timeout = timeout,
      warningTimeout = warningTimeout,
      onTimeout = () => alert("The request timed out. Please check your connection.", BannerLevel.Critical),
      onWarningTimeout = () => alert("The request is very long. Please check your connection.", BannerLevel.Regular),
      onFailed = t => alert(s"The request ${f} failed with error $t", BannerLevel.Critical)
    )

  def apply[O, R](
    r: CoreAPIClientImpl => Future[O],
    timeout:        FiniteDuration                     = 60 seconds,
    warningTimeout: FiniteDuration                     = 10 seconds)(action: O => R) =
    //import scala.concurrent.ExecutionContext.Implicits.global
//    org.openmole.gui.client.core.APIClient.uuid(()).future.onComplete { i => println("uuid " + i.get.uuid) }
    val f = future(r, timeout, warningTimeout)

    f.onComplete {
      case Success(r) ⇒ action(r)
      case _ =>
    }

