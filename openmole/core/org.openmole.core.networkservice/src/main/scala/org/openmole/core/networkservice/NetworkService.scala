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

import org.openmole.core.preference.{ ConfigurationLocation, Preference }

object NetworkService {

  val httpProxyEnabled = ConfigurationLocation("NetworkService", "HttpProxyEnabled", Some(false))
  val httpProxyHost = ConfigurationLocation("NetworkService", "HttpProxyHost", Option.empty[String])
  val httpProxyPort = ConfigurationLocation("NetworkService", "HttpProxyPort", Option.empty[Int])

  val httpsProxyEnabled = ConfigurationLocation("NetworkService", "HttpsProxyEnabled", Some(false))
  val httpsProxyHost = ConfigurationLocation("NetworkService", "HttpsProxyHost", Option.empty[String])
  val httpsProxyPort = ConfigurationLocation("NetworkService", "HttpsProxyPort", Option.empty[Int])

  def apply()(implicit preference: Preference) = new NetworkService

  case class HttpHost(host: String, port: Int, protocol: String)
}

class NetworkService(implicit preference: Preference) {

  /**
   * Returns the http proxy (if any) in the form
   * http://hostname:port
   */
  def httpProxyAsString(): Option[String] = {
    val isEnabledOpt: Option[Boolean] = preference.preferenceOption(NetworkService.httpProxyEnabled)
    val hostNameOpt: Option[String] = preference.preferenceOption(NetworkService.httpProxyHost)
    val portOpt: Option[Int] = preference.preferenceOption(NetworkService.httpProxyPort)
    (isEnabledOpt, hostNameOpt) match {
      case (_, Some(host: String)) if host.trim.isEmpty ⇒ None
      case (Some(false) | None, _)                      ⇒ None
      case (Some(true), Some(host))                     ⇒ Some("http://" + host + ":" + portOpt.getOrElse(80))
    }
  }

  /**
   * Returns the https proxy (if any) in the form
   * https://hostname:port
   */
  def httpsProxyAsString(): Option[String] = {
    val isEnabledOpt: Option[Boolean] = preference.preferenceOption(NetworkService.httpsProxyEnabled)
    val hostNameOpt: Option[String] = preference.preferenceOption(NetworkService.httpsProxyHost)
    val portOpt: Option[Int] = preference.preferenceOption(NetworkService.httpsProxyPort)
    (isEnabledOpt, hostNameOpt) match {
      case (_, Some(host: String)) if host.trim.isEmpty ⇒ None
      case (Some(false) | None, _)                      ⇒ None
      case (Some(true), Some(host))                     ⇒ Some("http://" + host + ":" + portOpt.getOrElse(443))
    }
  }

  /**
   * Returns an HttpHost (if any)
   * defined according to the configuration.
   */
  def httpProxyAsHost(): Option[NetworkService.HttpHost] = {
    val isEnabledOpt: Option[Boolean] = preference.preferenceOption(NetworkService.httpProxyEnabled)
    val hostNameOpt: Option[String] = preference.preferenceOption(NetworkService.httpProxyHost)
    val portOpt: Option[Int] = preference.preferenceOption(NetworkService.httpProxyPort)
    (isEnabledOpt, hostNameOpt) match {
      case (_, Some(host)) if host.trim.isEmpty ⇒ None
      case (Some(false), _)                     ⇒ None
      case (Some(true), Some(host))             ⇒ Some(NetworkService.HttpHost(host, portOpt.getOrElse(80), "http"))
    }
  }

}

