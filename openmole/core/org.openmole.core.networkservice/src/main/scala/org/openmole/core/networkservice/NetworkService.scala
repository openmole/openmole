/*
 * Copyright (C) 2018 Samuel Thiriot
 *  and 2022 Juste Raimbault
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

import org.openmole.core.preference.{Preference, PreferenceLocation}
import org.openmole.core.exception.UserBadDataError

import scala.io.Source
import org.apache.http
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import org.apache.http.client.methods.HttpGet
import org.apache.http.message.BasicHeader

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
      case _ => None
    }
  }

  def apply(hostURI: Option[String])(implicit preference: Preference) =
    new NetworkService(hostURI.map(HttpHost(_)).orElse(httpHostFromPreferences))

  case class HttpHost(hostURI: String) {
    def toHost: http.HttpHost = http.HttpHost.create(hostURI)
    override def toString: String = hostURI
  }


  /**
   * Simple http get with implicit NetworkService
   * @param url url
   * @param headers optional headers
   * @param networkService network service (proxy)
   * @return
   */
  def get(url: String,  headers: Seq[(String, String)] = Seq.empty)(implicit networkService: NetworkService): String = {
    val client = networkService.httpProxy match {
      case Some(httpHost: HttpHost) ⇒ HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).setProxy(httpHost.toHost).build()
      case _ ⇒ HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build()
    }
    val getReq = new HttpGet(url)
    headers.foreach{case (k,v) => getReq.setHeader(new BasicHeader(k, v))}
    val httpResponse = client.execute(getReq)
    if (httpResponse.getStatusLine.getStatusCode >= 300) throw new UserBadDataError(s"HTTP GET for $url responded with $httpResponse")
    val res = Source.fromInputStream(httpResponse.getEntity.getContent).mkString
    httpResponse.close()
    res
  }



}

class NetworkService(val httpProxy: Option[NetworkService.HttpHost])

