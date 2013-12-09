package org.openmole.web.misc.tools

import scalaj.http._
import java.net.URL
import java.util.UUID
import com.sun.net.ssl.{ SSLContext, TrustManagerFactory }
import javax.net.ssl._
import java.security.cert.{ CertificateException, X509Certificate, Certificate }
import org.openmole.misc.workspace.Workspace
import java.io.FileOutputStream
import org.openmole.web.misc.tools.XMLClient
import java.security.KeyStore

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 9/27/13
 * Time: 11:44 AM
 */

trait OMClientInterface {
  def address: String
  def path: String
  def pass: String
  def createMole(moleData: Array[Byte], context: Option[Array[Byte]] = None, pack: Boolean = false, encapsulate: Boolean = false): Any
  def getLoadedMoles: Any
  def getMoleStats(id: String): Any
  def startMole(id: String): Any
  def deleteMole(id: String): Any
}

trait AbstractOMClient extends OMClientInterface {
  case class HTTPControls(address: String, path: String, pass: String) extends OMClientInterface {
    lazy val apiKey = (Http.post(address + "/xml/getApiKey").header("pass", pass).option(HttpOptions.connTimeout(10000)).option(HttpOptions.readTimeout(10000)).asXml \ "apiKey").text

    def createMole(moleData: Array[Byte], context: Option[Array[Byte]], pack: Boolean = false, encapsulate: Boolean = false) = {
      val packVal = if (pack) "on" else ""
      val encapsulateVal = if (encapsulate) "on" else ""
      val url = s"$fullAddress/createMole"
      val multiparts = MultiPart("file", "", "", moleData) :: (context map (MultiPart("csv", "", "", _))).toList
      finishRequest(Http.multipart(url, multiparts: _*).header("apiKey", apiKey).params("pack" -> packVal, "encapsulate" -> encapsulateVal))
    }

    def getLoadedMoles = {
      val url = s"$fullAddress/execs"
      finishRequest(Http(url))
    }

    def getMoleStats(id: String) = {
      val url = s"$fullAddress/execs/$id"
      finishRequest(Http(url))
    }

    def startMole(id: String) = {
      val url = s"$fullAddress/start/$id"
      finishRequest(Http(url))
    }

    def deleteMole(id: String) = {
      val url = s"$fullAddress/remove/$id"
      finishRequest(Http(url).header("apiKey", apiKey))
    }

    private def finishRequest(h: Http.Request) = h.option(HttpOptions.sslSocketFactory(sslFactory)).option { case hS: HttpsURLConnection ⇒ hS.setHostnameVerifier(hostnameVerifier) case _ ⇒ () }.option { HttpOptions.connTimeout(10000) }.option { HttpOptions.readTimeout(10000) }
  }
  val ks = KeyStoreTools.getOMInsecureKeyStore
  def genSSLFactory(ks: KeyStore) = {
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(ks)
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, tmf.getTrustManagers, null)
    ctx.getSocketFactory()
  }

  val hostnameVerifier: HostnameVerifier = new HostnameVerifier {
    def verify(p1: String, p2: SSLSession): Boolean = true
  }

  private[tools] var sslFactory = genSSLFactory(ks)

  def trustCerts() = {
    val certs = Http(address).option(HttpOptions.allowUnsafeSSL).process(_ match {
      case h: HttpsURLConnection ⇒ h.getServerCertificates
      case _                     ⇒ throw new Exception("Can't trust a certificate that doesn't exist.")
    })
    val hostName = new URL(address).getHost
    certs foreach { println }
    ks.setCertificateEntry(hostName, certs(0))
    ks.store(new FileOutputStream(Workspace.file("OMUnsafeKeystore")), "".toCharArray)
    certs(0)
    //sslFactory = genSSLFactory(ks)
  }

  def areCertsTrusted = {
    val url = new URL(address)
    val hostName = url.getHost
    Http(address).option(HttpOptions.allowUnsafeSSL).process(_ match {
      case h: HttpsURLConnection ⇒ {
        val certs = h.getServerCertificates
        ks.getCertificate(hostName) == certs(0)
      }
      case _ ⇒ false
    })
  }

  val httpControls = new HTTPControls(address, path, pass)
  val fullAddress = if (address.endsWith("/")) address + path else address + "/" + path
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

  override def trustCerts() = {
    val cert = super.trustCerts()
    subClient.ks.setCertificateEntry(new URL(address).getHost, cert)
    subClient.sslFactory = genSSLFactory(ks)
    cert
  }
}

