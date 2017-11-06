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
import org.openmole.plugin.environment.batch.authentication._

//import java.net.URI
//import java.nio.file.FileSystems
import java.util.zip.GZIPInputStream
//
//import fr.iscpif.gridscale.authentication.P12Authentication
//import fr.iscpif.gridscale.egi._
//import fr.iscpif.gridscale.http._
import org.openmole.core.exception._
import org.openmole.core.fileservice._
//import org.openmole.plugin.environment.batch.authentication.CypheredPassword
//import org.openmole.plugin.environment.egi.EGIEnvironment._
import org.openmole.tool.file._
import org.openmole.tool.logger.Logger
import org.openmole.tool.stream._
//import org.openmole.core.authentication._
import org.openmole.core.preference._
//import org.openmole.core.serializer.SerializerService
import org.openmole.core.workspace.Workspace
//import org.openmole.plugin.environment.batch.environment.BatchEnvironment
//import org.openmole.tool.crypto.Cypher
import org.openmole.tool.tar.TarInputStream
//import squants.time.TimeConversions._
//
//import scala.util.Try
//
object EGIAuthentication extends Logger {

  import Log._

  val updatedFile = ".updated"

  def CACertificatesDir(implicit workspace: Workspace, preference: Preference): File =
    (workspace.persistentDir / "CACertificates").updateIfTooOld(preference(EGIEnvironment.CACertificatesCacheTime)) {
      caDir ⇒
        caDir.mkdir
        downloadCACertificates(preference(EGIEnvironment.CACertificatesSite), caDir)
    }

  def downloadCACertificates(address: String, dir: File)(implicit preference: Preference) = {
    import freedsl.dsl._
    implicit val httpIntepreter = gridscale.http.HTTPInterpreter()

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

    import cats.implicits._

    val dl = for {
      tarEntries ← gridscale.http.list[DSL](site, "/")
      _ ← tarEntries.traverse { tarEntry ⇒
        if (tarEntry.`type` != gridscale.FileType.Directory)
          gridscale.http.readStream[DSL, Unit](site, tarEntry.name, downloadCertificate(tarEntry.name))
        else ().pure[DSL]
      }
    } yield ()

    dl.eval
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
          val dn = (voms \ "X509Cert" \ "DN").headOption.map(_.text)

          s"voms://$host:${port}${dn.getOrElse("")}"
        }

        vomses.map(vomsUrl)
    }
  }
  //
  //  def update(a: EGIAuthentication, test: Boolean = true)(implicit cypher: Cypher, workspace: Workspace, authenticationStore: AuthenticationStore, serializerService: SerializerService) = {
  //    if (test) testPassword(a).get
  //    Authentication.set(a)
  //  }
  //
  //  def apply()(implicit workspace: Workspace, authenticationStore: AuthenticationStore, serializerService: SerializerService) =
  //    Authentication.allByCategory.
  //      getOrElse(classOf[EGIAuthentication].getName, Seq.empty).
  //      map(_.asInstanceOf[EGIAuthentication]).headOption
  //
  //  def clear(implicit workspace: Workspace, authenticationStore: AuthenticationStore) = Authentication.clear[EGIAuthentication]
  //
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
  //  def testPassword(a: EGIAuthentication)(implicit cypher: Cypher) =
  //    a match {
  //      case a: P12Certificate ⇒
  //        Try(P12Authentication.loadKeyStore(P12Authentication(a.certificate, a.password))).map(_ ⇒ true)
  //    }
  //
  //  def testProxy(a: EGIAuthentication, voName: String)(implicit cypher: Cypher, workspace: Workspace, preference: Preference) = {
  //    val vomses = EGIAuthentication.getVMOSOrError(voName)
  //    Try(initialise(a)(vomses, voName, None).apply()).map(_ ⇒ true)
  //  }
  //
  //  def testDIRACAccess(a: EGIAuthentication, voName: String)(implicit cypher: Cypher, services: BatchEnvironment.Services, workspace: Workspace) = {
  //    implicit val authentication = a
  //    Try(DIRACEnvironment(voName).jobService.getToken).map(_ ⇒ true)
  //  }
  //
  //  def DIRACVos = fr.iscpif.gridscale.egi.DIRACJobService.supportedVOs()
}

sealed trait EGIAuthentication

object P12Certificate {
  def apply(cypheredPassword: String, certificate: File = new File(new File(System.getProperty("user.home")), ".globus/certificate.p12")) =
    new P12Certificate(cypheredPassword, certificate)
}

class P12Certificate(val cypheredPassword: String, val certificate: File) extends EGIAuthentication with CypheredPassword