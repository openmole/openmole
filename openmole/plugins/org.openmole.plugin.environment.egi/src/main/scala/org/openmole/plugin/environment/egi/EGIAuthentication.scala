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

import java.io.{ File, _ }

import gridscale.authentication.P12Authentication
import gridscale.egi._
import org.openmole.core.authentication._
import org.openmole.core.serializer.SerializerService
import org.openmole.plugin.environment.batch.authentication._
import org.openmole.tool.crypto.Cypher
import java.util.zip.GZIPInputStream

import org.openmole.core.exception._
import org.openmole.core.fileservice._
import org.openmole.core.outputmanager.OutputManager
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.stream._
import org.openmole.core.preference._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.tar.TarInputStream

object EGIAuthentication extends JavaLogger {

  import Log._

  val updatedFile = ".updated"

  def CACertificatesDir(implicit workspace: Workspace, preference: Preference): File =
    (workspace.persistentDir / "CACertificates").updateIfTooOld(preference(EGIEnvironment.CACertificatesCacheTime)) {
      caDir ⇒
        caDir.mkdir
        downloadCACertificates(preference(EGIEnvironment.CACertificatesSite), caDir)
    }

  def downloadCACertificates(address: String, dir: File)(implicit preference: Preference) = {
    implicit val httpIntepreter = gridscale.http.HTTP()

    val site = gridscale.http.Server(address, preference(EGIEnvironment.CACertificatesDownloadTimeOut))

    def downloadCertificate(entryName: String)(is: InputStream) = {
      val tis = new TarInputStream(new GZIPInputStream(new BufferedInputStream(is)))

      try {
        val links = Iterator.continually(tis.getNextEntry).drop(1).takeWhile(_ != null).flatMap {
          tarEntry ⇒
            val destForName = new File(dir, tarEntry.getName)
            val dest = new File(dir, destForName.getName)

            if (dest.exists) dest.delete
            if (!tarEntry.getLinkName.isEmpty) Some(dest → tarEntry.getLinkName)
            else {
              tis.copy(dest)
              None
            }
        }.toList

        links.foreach {
          case (file, linkTo) ⇒ file.createLinkTo(linkTo)
        }
      }
      catch {
        case (e: IOException) ⇒ logger.log(WARNING, s"Unable to untar ${entryName} from $site", e)
      }
    }

    val tarEntries = gridscale.http.list(site, "/")
    tarEntries.foreach { tarEntry ⇒
      if (tarEntry.`type` != gridscale.FileType.Directory)
        gridscale.http.readStream[Unit](site, tarEntry.name, downloadCertificate(tarEntry.name))
    }
  }

  def voCards(implicit workspace: Workspace, preference: Preference) =
    (workspace.persistentDir / "voCards.xml").updateIfTooOld(preference(EGIEnvironment.VOCardCacheTime)) {
      voCards ⇒
        gridscale.http.getStream(preference(EGIEnvironment.VOInformationSite)) { _.copy(voCards) }
    }

  def getVOMS(vo: String)(implicit workspace: Workspace, preference: Preference): Option[Seq[String]] = getVOMS(vo, xml.XML.loadFile(voCards))
  def getVMOSOrError(vo: String)(implicit workspace: Workspace, preference: Preference) = getVOMS(vo).getOrElse(throw new UserBadDataError(s"ID card for VO $vo not found."))

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
          //val dn = (voms \ "X509Cert" \ "DN").headOption.map(_.text)
          //s"voms://$host:${port}${dn.getOrElse("")}"
          s"$host:$port"
        }

        vomses.map(vomsUrl)
    }
  }

  def update(a: EGIAuthentication, test: Boolean = true)(implicit cypher: Cypher, workspace: Workspace, authenticationStore: AuthenticationStore, serializerService: SerializerService) = {
    if (test) testPassword(a).get
    Authentication.set(a)
  }

  def apply()(implicit workspace: Workspace, authenticationStore: AuthenticationStore, serializerService: SerializerService) =
    Authentication.allByCategory.
      getOrElse(classOf[EGIAuthentication].getName, Seq.empty).
      map(_.asInstanceOf[EGIAuthentication]).headOption

  def clear(implicit workspace: Workspace, authenticationStore: AuthenticationStore) =
    Authentication.clear[EGIAuthentication]

  //  def initialise(a: EGIAuthentication)(
  //    serverURLs: Seq[String],
  //    voName:     String,
  //    fqan:       Option[String]
  //  )(implicit cypher: Cypher, workspace: Workspace, preference: Preference): () ⇒ GlobusAuthentication.Proxy =
  //    a match {
  //      case a: P12Certificate ⇒
  //        VOMSAuthentication.setCARepository(EGIAuthentication.CACertificatesDir)
  //        val p12 =
  //          P12VOMSAuthentication(
  //            P12Authentication(a.certificate, a.password),
  //            EGIEnvironment.proxyTime,
  //            serverURLs,
  //            voName,
  //            EGIEnvironment.proxyRenewalTime,
  //            fqan
  //          )
  //
  //        () ⇒ implicitly[GlobusAuthenticationProvider[P12VOMSAuthentication]].apply(p12)
  //    }
  //

  def proxy[A: EGIAuthenticationInterface](
    a:      A,
    voName: String,
    voms:   Option[Seq[String]],
    fqan:   Option[String])(implicit workspace: Workspace, preference: Preference): util.Try[gridscale.egi.VOMS.VOMSCredential] = EGI { implicits ⇒
    import implicits._

    def queryProxy(h: String) =
      gridscale.egi.VOMS.proxy(
        h,
        implicitly[EGIAuthenticationInterface[A]].apply(a),
        EGIAuthentication.CACertificatesDir,
        preference(EGIEnvironment.ProxyLifeTime),
        fqan,
        timeout = preference(EGIEnvironment.VOMSTimeout)
      )

    def vomses = voms orElse getVOMS(voName)

    util.Try(findWorking(vomses.map(_.toList).getOrElse(Nil), queryProxy, "VOMS server"))
  }

  def testPassword[A: EGIAuthenticationInterface](a: A)(implicit cypher: Cypher) =
    a match {
      case a: P12Certificate ⇒ P12Authentication.testPassword(P12Authentication(a.certificate, a.password))
    }

  def testProxy[A: EGIAuthenticationInterface](a: A, voName: String)(implicit workspace: Workspace, preference: Preference) =
    proxy(a, voName, None, None).map(_ ⇒ true)

  def testDIRACAccess[A: EGIAuthenticationInterface](a: A, voName: String)(implicit workspace: Workspace, preference: Preference) =
    util.Try(getToken(a, voName)).map(_ ⇒ true)

  def getToken[A: EGIAuthenticationInterface](a: A, voName: String)(implicit workspace: Workspace, preference: Preference) = EGI { implicits ⇒
    import implicits._
    import gridscale.dirac._
    val service = getService(voName, preference(EGIEnvironment.DiracConnectionTimeout))
    val s = server(service, implicitly[EGIAuthenticationInterface[A]].apply(a), CACertificatesDir)
    token(s)

  }

  def DIRACVos(implicit preference: Preference) = EGI { implicits ⇒
    import implicits._
    gridscale.dirac.supportedVOs(preference(EGIEnvironment.DiracConnectionTimeout))
  }

  implicit def defaultAuthentication(implicit workspace: Workspace, authenticationStore: AuthenticationStore, serializerService: SerializerService): EGIAuthentication =
    EGIAuthentication().getOrElse(throw new UserBadDataError("No authentication was found"))

}

sealed trait EGIAuthentication

object P12Certificate {
  def apply(cypheredPassword: String, certificate: File = new File(new File(System.getProperty("user.home")), ".globus/certificate.p12")) =
    new P12Certificate(cypheredPassword, certificate)
}

class P12Certificate(val cypheredPassword: String, val certificate: File) extends CypheredPassword with EGIAuthentication

object EGIAuthenticationInterface {
  implicit def p12CertificateIsEGIAuthentication(implicit cypher: Cypher): EGIAuthenticationInterface[EGIAuthentication] =
    new EGIAuthenticationInterface[EGIAuthentication] {
      override def apply(a: EGIAuthentication) =
        a match {
          case p12: P12Certificate ⇒ P12Authentication(p12.certificate, p12.password)
        }
    }
}

trait EGIAuthenticationInterface[A] {
  def apply(a: A): gridscale.authentication.P12Authentication
}