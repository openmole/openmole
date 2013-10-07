package org.openmole.web.misc.tools

import scalaj.http._
import java.net.URL

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 9/27/13
 * Time: 11:44 AM
 */
case class OMClient[T](address: String, responseFormat: ResponseFormat[T] = WebResponse) {
  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean = false, encapsulate: Boolean = false) = {
    val packVal = if (pack) "on" else ""
    val encapsulateVal = if (encapsulate) "on" else ""
    val url = address.toString + responseFormat.responseUrlSection + "/createMole"
    val multiparts = MultiPart("file", "", "", moleData) :: (context map (MultiPart("csv", "", "", _))).toList
    println(url)
    responseFormat.convertResponse(
      Http.multipart(url, multiparts: _*).params("pack" -> packVal, "encapsulate" -> encapsulateVal).option(HttpOptions.connTimeout(1000)).option(HttpOptions.readTimeout(5000))
    )
  }

  def getLoadedMoles = {
    val url = address.toString + responseFormat.responseUrlSection + "/execs"
    responseFormat.convertResponse(Http(url))
  }

  def getMoleStats(id: String) = {
    val url = address.toString + responseFormat.responseUrlSection + s"/execs/$id"
    responseFormat.convertResponse(Http(url))
  }

  def startMole(id: String) = {
    val url = address.toString + responseFormat.responseUrlSection + s"/start/$id"
    responseFormat.convertResponse(Http(url))
  }

  def deleteMole(id: String) = {
    val url = address.toString + responseFormat.responseUrlSection + s"/remove/$id"
    responseFormat.convertResponse(Http(url))
  }
}

trait ResponseFormat[T] {
  def response: String
  def responseUrlSection = if (response.isEmpty) "" else s"/$response"
  def convertResponse: scalaj.http.Http.Request ⇒ T
}

case object WebResponse extends ResponseFormat[String] {
  def response = ""
  def convertResponse = (a: scalaj.http.Http.Request) ⇒ a.asString
}

case object XMLResponse extends ResponseFormat[scala.xml.Elem] {
  def response = "xml"
  def convertResponse = (a: Http.Request) ⇒ a.asXml
}

