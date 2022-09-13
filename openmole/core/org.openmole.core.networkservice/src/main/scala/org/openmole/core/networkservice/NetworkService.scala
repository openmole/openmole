/*
 * Copyright (C) 2018 Samuel Thiriot
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

package org.openmole.core.networkservice

import org.openmole.core.networkservice.NetworkService.HttpHost
import org.openmole.core.preference.{ PreferenceLocation, Preference }

object NetworkService {

  val httpProxyEnabled = PreferenceLocation("NetworkService", "HttpProxyEnabled", Some(false))
  val httpProxyURI = PreferenceLocation("NetworkService", "httpProxyURI", Option.empty[String])

  def httpHostFromPreferences(implicit preference: Preference): Option[HttpHost] = {
    val isEnabledOpt: Option[Boolean] = preference.preferenceOption(NetworkService.httpProxyEnabled)
    val hostURIOpt: Option[String] = preference.preferenceOption(NetworkService.httpProxyURI)

    (isEnabledOpt, hostURIOpt) match {
      case (Some(false) | None, _)                            ⇒ None
      case (_, Some(hostURI: String)) if hostURI.trim.isEmpty ⇒ None
      case (_, Some(hostURI))                                 ⇒ Some(HttpHost(hostURI))
    }
  }

  def apply(hostURI: Option[String])(implicit preference: Preference) =
    new NetworkService(hostURI.map(HttpHost(_)).orElse(httpHostFromPreferences))

  case class HttpHost(hostURI: String)

  object HttpHost {
    def toString(host: HttpHost) = host.hostURI
  }

  //def get(url: String)

}

class NetworkService(val httpProxy: Option[HttpHost])

