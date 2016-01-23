/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.environment.egi

import java.io._
import java.net.URI
import java.util.UUID
import fr.iscpif.gridscale.authentication.{ PEMAuthentication, P12Authentication }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import java.nio.file.FileSystems
import java.util.zip.GZIPInputStream
import org.openmole.tool.file._
import org.openmole.core.workspace.{ Workspace, AuthenticationProvider }
import EGIEnvironment._
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl
import org.openmole.tool.logger.Logger
import org.openmole.tool.tar.TarInputStream
import scala.collection.JavaConversions._
import fr.iscpif.gridscale.egi._
import fr.iscpif.gridscale.http._
import fr.iscpif.gridscale._
import java.io.File
import scala.util.{ Success, Failure, Try }
import concurrent.duration._
import org.openmole.core.fileservice._

object EGIAuthentication extends Logger {

  import Log._

  val updatedFile = ".updated"

  def CACertificatesDir: File =
    Workspace.file("CACertificates").updateIfTooOld(Workspace.preferenceAsDuration(CACertificatesCacheTime)) {
      caDir ⇒
        caDir.mkdir
        downloadCACertificates(Workspace.preference(EGIEnvironment.CACertificatesSite), caDir)
    }

  def downloadCACertificates(address: String, dir: File) = {
    val fs = FileSystems.getDefault

    val site = HTTPStorage(url = address, Workspace.preferenceAsDuration(EGIEnvironment.CACertificatesDownloadTimeOut))
    for (tarUrl ← site.listNames("/")) {
      try {
        //val child = site.child(tarUrl)
        val is = site.openInputStream(tarUrl)

        val tis = new TarInputStream(new GZIPInputStream(new BufferedInputStream(is)))

        try {
          val links = Iterator.continually(tis.getNextEntry).drop(1).takeWhile(_ != null).flatMap {
            tarEntry ⇒
              val destForName = new File(dir, tarEntry.getName)
              val dest = new File(dir, destForName.getName)

              if (dest.exists) dest.delete
              if (!tarEntry.getLinkName.isEmpty) Some(dest -> tarEntry.getLinkName)
              else {
                tis.copy(dest)
                None
              }
          }.toList

          links.foreach {
            case (file, linkTo) ⇒ file.createLink(linkTo)
          }
        }
        catch {
          case (e: IOException) ⇒ logger.log(WARNING, "Unable to untar " + tarUrl, e)
        }
        finally tis.close
      }
      catch {
        case e: Throwable ⇒ throw new IOException(tarUrl, e)
      }
    }
  }

  def voCards =
    Workspace.file("voCards.xml").updateIfTooOld(Workspace.preferenceAsDuration(VOCardCacheTime)) {
      voCards ⇒
        HTTPStorage.withConnection(
          new URI(Workspace.preference(EGIEnvironment.VOInformationSite)),
          Workspace.preferenceAsDuration(EGIEnvironment.VOCardDownloadTimeOut)) { http ⇒
            val is: InputStream = http.getInputStream
            try is.copy(voCards)
            finally is.close
          }
    }

  def getVOMS(vo: String): Option[Seq[String]] = getVOMS(vo, xml.XML.loadFile(voCards))
  def getVMOSOrError(vo: String) = getVOMS(vo).getOrElse(throw new UserBadDataError(s"ID card for VO $vo not found."))

  def getVOMS(vo: String, x: xml.Node) = {
    import xml._

    def attributeIsDefined(name: String, value: String) =
      (_: Node).attribute(name).filter(_.text == value).isDefined

    val card = (x \ "IDCard" filter (attributeIsDefined("Name", vo))).headOption

    card map {
      card ⇒
        val vomses = (card \ "gLiteConf" \ "VOMSServers" \ "VOMS_Server")

        def vomsUrl(voms: Node) = {
          val host = (voms \ "hostname").head.text
          val port = (voms.attribute("VomsesPort").get.text)
          val dn = (voms \ "X509Cert" \ "DN").headOption.map(_.text)

          s"voms://$host:${port}${dn.getOrElse("")}"
        }

        vomses.map(vomsUrl)
    }
  }

  def update(a: EGIAuthentication) = Workspace.authentications.set(a)
  def apply()(implicit authentications: AuthenticationProvider) = authentications(classOf[EGIAuthentication]).headOption
  def clear() = Workspace.authentications.clear[EGIAuthentication]

  def initialise(a: EGIAuthentication)(
    serverURLs: Seq[String],
    voName: String,
    fqan: Option[String])(implicit authenticationProvider: AuthenticationProvider): () ⇒ GlobusAuthentication.Proxy =
    a match {
      case a: P12Certificate ⇒
        VOMSAuthentication.setCARepository(EGIAuthentication.CACertificatesDir)
        val p12 =
          P12VOMSAuthentication(
            P12Authentication(a.certificate, a.password(authenticationProvider)),
            EGIEnvironment.proxyTime,
            serverURLs,
            voName,
            EGIEnvironment.proxyRenewalRatio,
            fqan)

        () ⇒ implicitly[GlobusAuthenticationProvider[P12VOMSAuthentication]].apply(p12)
        case a: PEMCertificate ⇒
        VOMSAuthentication.setCARepository(EGIAuthentication.CACertificatesDir)
        val pem = PEMVOMSAuthentication(
          PEMAuthentication(a.certificate, a.key, a.password(authenticationProvider)),
          EGIEnvironment.proxyTime,
          serverURLs,
          voName,
          EGIEnvironment.proxyRenewalRatio,
          fqan
        )

        () ⇒ implicitly[GlobusAuthenticationProvider[PEMVOMSAuthentication]].apply(pem)
      /*case a: ProxyFile ⇒
        val proxy = ProxyFileAuthentication(a.proxy)
        () ⇒ implicitly[GlobusAuthenticationProvider[ProxyFileAuthentication]].apply(proxy)*/
    }

}

trait EGIAuthentication