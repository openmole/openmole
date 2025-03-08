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
import org.openmole.core.exception.InternalProcessingError

import scala.io.Source
import org.apache.http
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.message.BasicHeader
import org.bouncycastle.mime.Headers

import java.io.InputStream
import java.net.URI

object NetworkService:

  val httpProxyEnabled = PreferenceLocation("NetworkService", "HttpProxyEnabled", Some(false))
  val httpProxyURI = PreferenceLocation("NetworkService", "httpProxyURI", Option.empty[String])

  def httpHostFromPreferences(implicit preference: Preference): Option[HttpHost] = {
    val isEnabledOpt: Option[Boolean] = preference.preferenceOption(NetworkService.httpProxyEnabled)
    val hostURIOpt: Option[String] = preference.preferenceOption(NetworkService.httpProxyURI)

    (isEnabledOpt, hostURIOpt) match {
      case (Some(false) | None, _) => None
      case (_, Some(hostURI: String)) if hostURI.trim.isEmpty => None
      case (_, Some(hostURI)) => Some(HttpHost(hostURI))
      case _ => None
    }
  }

  def apply(hostURI: Option[String])(implicit preference: Preference) =
    new NetworkService(hostURI.map(HttpHost(_)).orElse(httpHostFromPreferences))

  case class HttpHost(hostURI: String) {
    /**
     * Convert to a HttpHost
     * Trailing slash is removed if needed
     * @return
     */
    def toHost: http.HttpHost = http.HttpHost.create(hostURI.substring(0, hostURI.length() - (if (hostURI.endsWith("/")) 1 else 0)))

    override def toString: String = hostURI
  }


  /**
   * Simple http get with implicit NetworkService
   *  Apache HttpClient works transparently with https, no need to add a custom protocol here https://hc.apache.org/httpcomponents-client-5.1.x/
   *
   * @param url            url
   * @param headers        optional headers
   * @param networkService network service (proxy)
   * @return
   */
  def get(url: String, headers: Seq[(String, String)] = Seq.empty)(implicit networkService: NetworkService): String = {
    val is = getInputStream(url, headers)
    val res = Source.fromInputStream(is).mkString
    is.close()
    res
  }

  private def newClient(using networkService: NetworkService) =
    networkService.httpProxy match {
      case Some(httpHost: HttpHost) => HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).setProxy(httpHost.toHost).build()
      case _ => HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build()
    }

  def withResponse[T](url: String, headers: Seq[(String, String)] = Seq.empty)(f: CloseableHttpResponse => T)(using NetworkService): T =
    val client = newClient
    val getReq = new HttpGet(url)
    headers.foreach { (k, v) => getReq.setHeader(new BasicHeader(k, v)) }
    try
      val httpResponse = client.execute(getReq)
      try f(httpResponse)
      finally httpResponse.close()
    catch
      case t: Throwable => throw new InternalProcessingError(s"HTTP GET for $url failed", t)
    finally client.close()

  def getInputStream(url: String, headers: Seq[(String, String)] = Seq.empty)(using NetworkService): InputStream =
    val client = newClient
    val getReq = new HttpGet(url)
    headers.foreach{ (k,v) => getReq.setHeader(new BasicHeader(k, v)) }
    try {
      val httpResponse = client.execute(getReq)
      if (httpResponse.getStatusLine.getStatusCode >= 300) throw new InternalProcessingError(s"HTTP GET for $url responded with $httpResponse")
      httpResponse.getEntity.getContent
    } catch case t: Throwable => throw new InternalProcessingError(s"HTTP GET for $url failed", t)
    //finally client.close()


  def urlProtocol(url: String): String = new URI(url).getScheme

  def proxyVariables(using networkService: NetworkService) =
    networkService.httpProxy match
      case Some(proxy) =>
        Seq(
          "http_proxy" -> proxy.hostURI,
          "HTTP_PROXY" -> proxy.hostURI,
          "https_proxy" -> proxy.hostURI,
          "HTTPS_PROXY" -> proxy.hostURI
        )
      case None => Seq()


class NetworkService(val httpProxy: Option[NetworkService.HttpHost])

