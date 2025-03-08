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
import gridscale.http.HTTP
import org.openmole.core.authentication._
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
import org.openmole.tool.archive.*

import io.circe.generic.auto.*
import org.openmole.core.json.given

object EGIAuthentication extends JavaLogger {

  import Log._

  val updatedFile = ".updated"

  def CACertificatesDir(implicit workspace: Workspace, preference: Preference): File =
    (workspace.persistentDir / "CACertificates").updateIfTooOld(preference(EGIEnvironment.CACertificatesCacheTime)) {
      caDir =>
        caDir.mkdir
        downloadCACertificates(preference(EGIEnvironment.CACertificatesSite), caDir)
    }

  def downloadCACertificates(address: String, dir: File)(implicit preference: Preference) =
    implicit val httpIntepreter = gridscale.http.HTTP()

    val site = gridscale.http.Server(address, preference(EGIEnvironment.CACertificatesDownloadTimeOut))

    def downloadCertificate(entryName: String)(is: InputStream) =
      val tis = TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(is)))
      try tis.extract(dir)
      finally tis.close()

    val tarEntries = gridscale.http.list(site, "/")
    tarEntries.foreach: tarEntry =>
      if (tarEntry.`type` != gridscale.FileType.Directory)
        gridscale.http.readStream[Unit](site, tarEntry.name, downloadCertificate(tarEntry.name))

    // Flatten extracted archives
    for
      file <- dir.listFiles()
      if file.isDirectory
    do
      val tmpDirectory = file.getParentFile / (file.getName  + ".tmp")
      file move tmpDirectory
      tmpDirectory.listFiles.foreach: f =>
        f move (dir / f.getName)
      tmpDirectory.delete()


  def getVOMS(vo: String)(implicit workspace: Workspace, preference: Preference): Option[Seq[String]] =
    gridscale.egi.VOMS.get(vo, preference(EGIEnvironment.VOPortalAPIKey))

  def getVMOSOrError(vo: String)(implicit workspace: Workspace, preference: Preference) =
    getVOMS(vo).getOrElse(throw new UserBadDataError(s"No ID card for VO $vo found on VO portal."))

  def update(a: EGIAuthentication, test: Boolean = true)(implicit cypher: Cypher, workspace: Workspace, authenticationStore: AuthenticationStore) =
    if test then testPassword(a).get
    a match
      case a: P12Certificate => Authentication.save[EGIAuthentication](a.copy(cypheredPassword = cypher.encrypt(a.cypheredPassword)), (_, _) => true)

  def apply()(implicit workspace: Workspace, authenticationStore: AuthenticationStore, cypher: Cypher) =
    Authentication.load[EGIAuthentication].headOption.map:
      case a: P12Certificate => a.copy(cypheredPassword = cypher.decrypt(a.cypheredPassword))

  def clear(implicit workspace: Workspace, authenticationStore: AuthenticationStore) =
    Authentication.clear[EGIAuthentication]

  //  def initialise(a: EGIAuthentication)(
  //    serverURLs: Seq[String],
  //    voName:     String,
  //    fqan:       Option[String]
  //  )(implicit cypher: Cypher, workspace: Workspace, preference: Preference): () => GlobusAuthentication.Proxy =
  //    a match {
  //      case a: P12Certificate =>
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
  //        () => implicitly[GlobusAuthenticationProvider[P12VOMSAuthentication]].apply(p12)
  //    }
  //

  def proxy[A: EGIAuthenticationInterface](
    a:      A,
    voName: String,
    voms:   Option[String],
    fqan:   Option[String])(implicit workspace: Workspace, preference: Preference): util.Try[gridscale.egi.VOMS.VOMSCredential] =
    HTTP.withHTTP:
      def queryProxy(h: String) =
        gridscale.egi.VOMS.proxy(
          h,
          implicitly[EGIAuthenticationInterface[A]].apply(a),
          EGIAuthentication.CACertificatesDir,
          preference(EGIEnvironment.ProxyLifeTime),
          fqan,
          timeout = preference(EGIEnvironment.VOMSTimeout)
        )

      def vomses = voms.map(v => Seq(v)) orElse getVOMS(voName)

      vomses match
        case Some(vomses) => util.Try(findFirstWorking(vomses)(queryProxy, "VOMS server"))
        case None         => util.Failure(new UserBadDataError(s"No VOMS server found for VO $voName"))

  def testPassword[A: EGIAuthenticationInterface](a: A) =
    a match
      case a: P12Certificate => P12Authentication.testPassword(P12Authentication(a.certificate, a.password))

  def testProxy[A: EGIAuthenticationInterface](a: A, voName: String)(implicit workspace: Workspace, preference: Preference) =
    proxy(a, voName, None, None).map(_ => true)

  def testDIRACAccess[A: EGIAuthenticationInterface](a: A, voName: String)(implicit workspace: Workspace, preference: Preference) =
    util.Try(getToken(a, voName)).map(_ => true)

  def getToken[A: EGIAuthenticationInterface](a: A, voName: String)(implicit workspace: Workspace, preference: Preference) =
    HTTP.withHTTP:
      import gridscale.dirac._
      val service = getService(voName, CACertificatesDir, preference(EGIEnvironment.DiracConnectionTimeout))
      val s = server(service, implicitly[EGIAuthenticationInterface[A]].apply(a), CACertificatesDir)
      token(s)

  def DIRACVos(implicit workspace: Workspace, preference: Preference) =
    HTTP.withHTTP:
      gridscale.dirac.supportedVOs(CACertificatesDir, preference(EGIEnvironment.DiracConnectionTimeout))

  implicit def defaultAuthentication(implicit workspace: Workspace, authenticationStore: AuthenticationStore, cypher: Cypher): EGIAuthentication =
    EGIAuthentication().getOrElse(throw new UserBadDataError("No authentication was found"))

}

sealed trait EGIAuthentication

object P12Certificate:
  def apply(cypheredPassword: String, certificate: File = new File(new File(System.getProperty("user.home")), ".globus/certificate.p12")) =
    new P12Certificate(cypheredPassword, certificate)

case class P12Certificate(private[egi] val cypheredPassword: String, certificate: File) extends EGIAuthentication:
  def password = cypheredPassword

object EGIAuthenticationInterface:
  implicit def p12CertificateIsEGIAuthentication: EGIAuthenticationInterface[EGIAuthentication] =
    case p12: P12Certificate => P12Authentication(p12.certificate, p12.password)

trait EGIAuthenticationInterface[A]:
  def apply(a: A): gridscale.authentication.P12Authentication
