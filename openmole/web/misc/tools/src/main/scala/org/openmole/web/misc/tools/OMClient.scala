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

trait AbstractOMClient extends Any {
  implicit class HTTPControls(address: URL) extends AbstractOMClient {
    def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean = false, encapsulate: Boolean = false) = {
      val packVal = if (pack) "on" else ""
      val encapsulateVal = if (encapsulate) "on" else ""
      val url = address.toString + "/createMole"
      val multiparts = MultiPart("file", "", "", moleData) :: (context map (MultiPart("csv", "", "", _))).toList
      Http.multipart(url, multiparts: _*).params("pack" -> packVal, "encapsulate" -> encapsulateVal).option(HttpOptions.connTimeout(10000)).option(HttpOptions.readTimeout(10000))
    }

    def getLoadedMoles = {
      val url = address.toString + "/execs"
      Http(url)
    }

    def getMoleStats(id: String) = {
      val url = address.toString + s"/execs/$id"
      Http(url)
    }

    def startMole(id: String) = {
      val url = address.toString + s"/start/$id"
      Http(url)
    }

    def deleteMole(id: String) = {
      val url = address.toString + s"/remove/$id"
      Http(url)
    }
  }

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]] = None, pack: Boolean = false, encapsulate: Boolean = false): Any
  def getLoadedMoles: Any
  def getMoleStats(id: String): Any
  def startMole(id: String): Any
  def deleteMole(id: String): Any
}

case class WebClient(_address: String) extends AbstractOMClient {
  val address = new URL(_address)

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean): String = {
    address.createMole(moleData, context, pack, encapsulate).asString
  }

  def getLoadedMoles = address.getLoadedMoles.asString

  def getMoleStats(id: String) = address.getMoleStats(id).asString

  def startMole(id: String) = address.startMole(id).asString

  def deleteMole(id: String) = address.deleteMole(id).asString
}

case class XMLClient(_address: String) extends AbstractOMClient {
  val address = new URL(_address + (if (_address.endsWith("/")) "xml" else "/xml"))

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean) = address.createMole(moleData, context, pack, encapsulate).asXml

  def getLoadedMoles = address.getLoadedMoles.asXml

  def getMoleStats(id: String) = address.getMoleStats(id).asXml

  def startMole(id: String) = address.startMole(id).asXml

  def deleteMole(id: String) = address.deleteMole(id).asXml
}

case class ScalaClient(_address: String) extends AbstractOMClient {
  val subClient = XMLClient(_address)

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean): Either[UUID, String] = {
    val xml = subClient.createMole(moleData, context, pack, encapsulate) \ "moleID"
    (xml \ "moleID").headOption map (s ⇒ Left(UUID.fromString(s.text))) getOrElse Right(xml \ "error" text)
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

