package org.openmole.web.misc.tools

import org.openmole.core.workspace.Workspace

import scalaj.http._
import java.net.URL
import java.util.UUID
import com.sun.net.ssl.{ SSLContext, TrustManagerFactory }
import javax.net.ssl._
import java.security.cert.{ CertificateException, X509Certificate, Certificate }
import java.io.FileOutputStream
import java.security.KeyStore

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 9/27/13
 * Time: 11:44 AM
 */

//TODO: There's so much to fix in this class. Consult romain and mathieu about it.

trait RESTClientInterface {
  def address: String
  def path: String
  def createMole(moleData: Array[Byte], context: Option[Array[Byte]] = None, pack: Boolean = false, encapsulate: Boolean = false): Any
  def getLoadedMoles: Any
  def getMoleStats(id: String): Any
  def startMole(id: String): Any
  def deleteMole(id: String): Any
  def trustCert(): Unit
  def isCertTrusted: Option[Boolean]
  def cert: Option[Certificate]
  val fullAddress = if (address.endsWith("/")) address + path else address + "/" + path
}

class HTTPControls(val address: String, val path: String, pass: String) extends RESTClientInterface {
  private val ks = KeyStoreTools.getOMInsecureKeyStore

  private val hostnameVerifier: HostnameVerifier = new HostnameVerifier {
    def verify(p1: String, p2: SSLSession): Boolean = true
  }

  private var sslFactory = genSSLFactory(ks)

  lazy val apiKey = {
    val res = addDefaultOptions(Http.post(address + "/xml/getApiKey").header("pass", pass)).asXml
    res.label match {
      case "apiKey" ⇒ res.text
      case "error"  ⇒ throw new Exception(s"Invalid password given: ${(res \ "message").text}")
    }
  }

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean = false, encapsulate: Boolean = false) = {
    val packVal = if (pack) "on" else ""
    val encapsulateVal = if (encapsulate) "on" else ""
    val url = s"$fullAddress/createMole"
    val multiparts = MultiPart("file", "", "", moleData) :: (context map (MultiPart("csv", "", "", _))).toList
    addDefaultOptions(Http.multipart(url, multiparts: _*).header("apiKey", apiKey).params("pack" -> packVal, "encapsulate" -> encapsulateVal))
  }

  def getLoadedMoles = {
    val url = s"$fullAddress/execs"
    addDefaultOptions(Http(url))
  }

  def getMoleStats(id: String) = {
    val url = s"$fullAddress/execs/$id"
    addDefaultOptions(Http(url))
  }

  def startMole(id: String) = {
    val url = s"$fullAddress/start/$id"
    addDefaultOptions(Http(url))
  }

  def deleteMole(id: String) = {
    val url = s"$fullAddress/remove/$id"
    addDefaultOptions(Http(url).header("apiKey", apiKey))
  }

  private def addDefaultOptions(h: Http.Request) = h.option(HttpOptions.sslSocketFactory(sslFactory)).option { case hS: HttpsURLConnection ⇒ hS.setHostnameVerifier(hostnameVerifier) case _ ⇒ () }.option { HttpOptions.connTimeout(30000) }.option { HttpOptions.readTimeout(30000) }

  private def genSSLFactory(ks: KeyStore) = {
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(ks)
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, tmf.getTrustManagers, null)
    ctx.getSocketFactory()
  }

  @throws[CertificateException]
  def trustCert() = {
    val serverCert = cert getOrElse (throw new CertificateException("Cannot trust a non-existent certificate"))

    ks.setCertificateEntry(hostname, serverCert)
    ks.store(new FileOutputStream(Workspace.file("OMUnsafeKeystore")), "".toCharArray) //make some kind of managed keystore so this kind of code is unnecessary
    sslFactory = genSSLFactory(ks)
  }

  def isCertTrusted = cert map (_ == ks.getCertificate(hostname))

  lazy val hostname = new URL(address).getHost

  def cert = addDefaultOptions(Http(address)).option(HttpOptions.allowUnsafeSSL).process(_ match {
    case h: HttpsURLConnection ⇒ {
      val certs = h.getServerCertificates
      Some(certs(0))
    }
    case _ ⇒ None
  })
}

abstract class AbstractRESTClient(val path: String, pass: String) extends RESTClientInterface {
  def trustCert() = httpControls.trustCert()
  def isCertTrusted = httpControls.isCertTrusted
  def cert = httpControls.cert

  val httpControls: HTTPControls = new HTTPControls(address, path, pass)
}

class WebClient(val address: String, pass: String) extends AbstractRESTClient(WebClient.path, pass) {
  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean): String = {
    httpControls.createMole(moleData, context, pack, encapsulate).asString
  }

  def getLoadedMoles = httpControls.getLoadedMoles.asString

  def getMoleStats(id: String) = httpControls.getMoleStats(id).asString

  def startMole(id: String) = httpControls.startMole(id).asString

  def deleteMole(id: String) = httpControls.deleteMole(id).asString
}

object WebClient {
  val path = ""
}

class XMLClient(val address: String, pass: String) extends AbstractRESTClient(XMLClient.path, pass) {
  //val path = "xml"

  def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean, encapsulate: Boolean) = httpControls.createMole(moleData, context, pack, encapsulate).asXml

  def getLoadedMoles = httpControls.getLoadedMoles.asXml

  def getMoleStats(id: String) = httpControls.getMoleStats(id).asXml

  def startMole(id: String) = httpControls.startMole(id).asXml

  def deleteMole(id: String) = httpControls.deleteMole(id).asXml
}

object XMLClient {
  val path = "xml"
}

class ScalaClient(val address: String, pass: String) extends AbstractRESTClient(XMLClient.path, pass) {
  val subClient = new XMLClient(address, pass)
  //val path = subClient.path

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

  override val httpControls: HTTPControls = subClient.httpControls
}