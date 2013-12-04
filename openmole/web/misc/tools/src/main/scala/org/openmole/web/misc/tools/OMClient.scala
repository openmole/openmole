package org.openmole.web.misc.tools

import scalaj.http._
import java.net.URL
import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 9/27/13
 * Time: 11:44 AM
 */

trait AbstractOMClient {
  case class HTTPControls(address: String, path: String, pass: String) extends AbstractOMClient {
    lazy val apiKey = (Http.post(address + "/xml/getApiKey").header("pass", pass).option(HttpOptions.connTimeout(10000)).option(HttpOptions.readTimeout(10000)).asXml \ "api-key").text

    def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean = false, encapsulate: Boolean = false) = {
      val packVal = if (pack) "on" else ""
      val encapsulateVal = if (encapsulate) "on" else ""
      val url = s"$fullAddress/createMole"
      val multiparts = MultiPart("file", "", "", moleData) :: (context map (MultiPart("csv", "", "", _))).toList
      Http.multipart(url, multiparts: _*).header("apiKey", apiKey).params("pack" -> packVal, "encapsulate" -> encapsulateVal).option(HttpOptions.connTimeout(10000)).option(HttpOptions.readTimeout(10000))
    }

    def getLoadedMoles = {
      val url = s"$fullAddress/execs"
      Http(url)
    }

    def getMoleStats(id: String) = {
      val url = s"$fullAddress/execs/$id"
      Http(url)
    }

    def startMole(id: String) = {
      val url = s"$fullAddress/start/$id"
      Http(url).header("apiKey", apiKey)
    }

    def deleteMole(id: String) = {
      val url = s"$fullAddress/remove/$id"
      Http(url).header("apiKey", apiKey)
    }
  }

  def address: String
  def path: String
  def pass: String
  lazy val httpControls = new HTTPControls(address, path, pass)
  val fullAddress = if (address.endsWith("/")) address + path else address + "/" + path
  def createMole(moleData: Array[Byte], context: Option[Array[Byte]] = None, pack: Boolean = false, encapsulate: Boolean = false): Any
  def getLoadedMoles: Any
  def getMoleStats(id: String): Any
  def startMole(id: String): Any
  def deleteMole(id: String): Any
}

case class WebClient(address: String, pass: String) extends AbstractOMClient {
  def path = ""

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean): String = {
    httpControls.createMole(moleData, context, pack, encapsulate).asString
  }

  def getLoadedMoles = httpControls.getLoadedMoles.asString

  def getMoleStats(id: String) = httpControls.getMoleStats(id).asString

  def startMole(id: String) = httpControls.startMole(id).asString

  def deleteMole(id: String) = httpControls.deleteMole(id).asString
}

case class XMLClient(address: String, pass: String) extends AbstractOMClient {
  def path = "xml"

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean) = httpControls.createMole(moleData, context, pack, encapsulate).asXml

  def getLoadedMoles = httpControls.getLoadedMoles.asXml

  def getMoleStats(id: String) = httpControls.getMoleStats(id).asXml

  def startMole(id: String) = httpControls.startMole(id).asXml

  def deleteMole(id: String) = httpControls.deleteMole(id).asXml
}

case class ScalaClient(address: String, pass: String) extends AbstractOMClient {
  def path = "xml"
  val subClient = XMLClient(address, pass)

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean): Either[String, UUID] = {
    val xml = subClient.createMole(moleData, context, pack, encapsulate)
    if (xml.label == "moleID") Right(UUID.fromString(xml.text)) else Left(xml.text)
  }

  def getLoadedMoles = {
    val xml = subClient.getLoadedMoles

    xml \ "moleID" map (s ⇒ UUID.fromString(s.text))
  }

  def getMoleStats(id: String) = {
    val xml = subClient.getMoleStats(id)

    xml \ "stat" map (stat ⇒ (stat \ "@id" text) -> stat.text.toInt) toMap
  }

  def startMole(id: String) = {
    val xml = subClient.startMole(id)

    xml \ "@status" text
  }

  def deleteMole(id: String) = {
    val xml = subClient.deleteMole(id)

    xml \ "@status" text
  }
}

