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
import org.openmole.core.preference.{ ConfigurationLocation, Preference }

object NetworkService {

  val httpProxyEnabled = ConfigurationLocation("NetworkService", "HttpProxyEnabled", Some(false))
  val httpProxyHost = ConfigurationLocation("NetworkService", "HttpProxyHost", Option.empty[String])

  def httpHostFromPreferences(implicit preference: Preference): Option[HttpHost] = {
    val isEnabledOpt: Option[Boolean] = preference.preferenceOption(NetworkService.httpProxyEnabled)
    val hostNameOpt: Option[String] = preference.preferenceOption(NetworkService.httpProxyHost)

    (isEnabledOpt, hostNameOpt) match {
      case (Some(false) | None, _)                      ⇒ None
      case (_, Some(host: String)) if host.trim.isEmpty ⇒ None
      case (_, Some(host))                              ⇒ Some(HttpHost(host))
    }
  }

  def apply(httpHost: Option[String] = None)(implicit preference: Preference) =
    new NetworkService(httpHost.map(HttpHost(_)).orElse(httpHostFromPreferences))

  case class HttpHost(host: String)

  object HttpHost {
    def isHttps(host: HttpHost) = {
      new java.net.URL(host.host).getProtocol == "https"
    }

    def toString(host: HttpHost) = host.host
  }
}

case class NetworkService(httpProxy: Option[HttpHost])

