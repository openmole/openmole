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

package org.openmole.plugin.environment.glite

import com.ice.tar.TarInputStream
import fr.iscpif.gridscale.storage._
import java.io._
import java.net.URI
import org.openmole.misc.tools.service.Logger
import java.nio.file.FileSystems
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.zip.GZIPInputStream
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.authentication._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.Workspace
import GliteEnvironment._
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl
import scala.collection.JavaConversions._
import scala.ref.WeakReference
import scala.Some
import scala.Some
import fr.iscpif.gridscale.authentication._
import java.io.File

object GliteAuthentication extends Logger {

  lazy val CACertificatesDir: File = {
    Workspace.file("ca.lock").withLock { _ ⇒
      val caDir = Workspace.file("CACertificates")

      if (!caDir.exists || !new File(caDir, ".complete").exists) {
        caDir.mkdir
        downloadCACertificates(Workspace.preference(GliteEnvironment.CACertificatesSite), caDir)
        new File(caDir, ".complete").createNewFile
      }
      caDir
    }
  }

  def downloadCACertificates(address: String, dir: File) = {
    val fs = FileSystems.getDefault

    val site = new HTTPStorage {
      val url = address
    }
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

  def voCards = {
    val voCards = Workspace.file("voCards.xml")
    if (!voCards.exists) {
      val tmpVoCards = Workspace.newFile
      HTTPStorage.withConnection(
        new URI(Workspace.preference(GliteEnvironment.VOInformationSite)),
        Workspace.preferenceAsDuration(GliteEnvironment.VOCardDownloadTimeOut).toSeconds) { http ⇒
          val is: InputStream = http.getInputStream
          try is.copy(tmpVoCards)
          finally is.close
          tmpVoCards move voCards
        }
    }
    voCards
  }

  def getVOMS(vo: String): Option[String] = getVOMS(vo, xml.XML.loadFile(voCards))
  def getVMOSOrError(vo: String) = getVOMS(vo).getOrElse(throw new UserBadDataError(s"ID card for VO $vo not found."))

  def getVOMS(vo: String, x: xml.Node) = {
    import xml._

    def attributeIsDefined(name: String, value: String) =
      (_: Node).attribute(name).filter(_.text == value).isDefined

    val card = (x \ "IDCard" filter (attributeIsDefined("Name", vo))).headOption

    card map {
      card ⇒
        val voms = (card \ "gLiteConf" \ "VOMSServers" \ "VOMS_Server").head
        val host = (voms \ "hostname").head.text
        val port = (voms.attribute("VomsesPort").get.text)
        val dn = (voms \ "X509Cert" \ "DN").headOption.map(_.text)

        s"voms://$host:${port}${dn.getOrElse("")}"
    }
  }

  def update(a: GliteAuthentication) = Workspace.persistentList(classOf[GliteAuthentication])(0) = a
  def apply() = Workspace.persistentList(classOf[GliteAuthentication])(0)
  def get = Workspace.persistentList(classOf[GliteAuthentication]).get(0)

  def initialise(a: GliteAuthentication)(
    serverURL: String,
    voName: String,
    proxyFile: File,
    lifeTime: Int,
    fqan: Option[String]) =
    a match {
      case a: P12Certificate ⇒
        VOMSAuthentication.setCARepository(GliteAuthentication.CACertificatesDir)
        val (_serverURL, _voName, _proxyFile, _lifeTime, _fqan) = (serverURL, voName, proxyFile, lifeTime, fqan)
        new P12VOMSAuthentication {
          val certificate = a.certificate
          val serverURL = _serverURL
          val voName = _voName
          val proxyFile = _proxyFile
          val lifeTime = _lifeTime
          val password = a.password
          override val fqan = _fqan
        }
      case a: PEMCertificate ⇒
        VOMSAuthentication.setCARepository(GliteAuthentication.CACertificatesDir)
        val (_serverURL, _voName, _proxyFile, _lifeTime, _fqan) = (serverURL, voName, proxyFile, lifeTime, fqan)
        new PEMVOMSAuthentication {
          val certificate = a.certificate
          val key = a.key
          val serverURL = _serverURL
          val voName = _voName
          val proxyFile = _proxyFile
          val lifeTime = _lifeTime
          val password = a.password
          override val fqan = _fqan
        }
      case a: ProxyFile ⇒
        new ProxyFileAuthentication {
          val proxy = a.proxy
        }
    }

}

trait GliteAuthentication