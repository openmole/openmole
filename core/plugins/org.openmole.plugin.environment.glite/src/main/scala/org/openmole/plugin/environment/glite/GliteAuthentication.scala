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

object GliteAuthentication extends Logger {

  lazy val CACertificatesDir: File = {
    Workspace.file("ca.lock").withLock { _ =>
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
        } catch {
          case (e: IOException) ⇒ logger.log(WARNING, "Unable to untar " + tarUrl, e)
        } finally tis.close
      } catch {
        case e: Throwable ⇒ throw new IOException(tarUrl, e)
      }
    }
  }

  def voCards = {
      val voCards = Workspace.file("voCards.xml")
      if(!voCards.exists) {
        val tmpVoCards = Workspace.newFile
        HTTPStorage.withConnection(
          new URI(Workspace.preference(GliteEnvironment.VOInformationSite)),
            Workspace.preferenceAsDuration(GliteEnvironment.VOCardDownloadTimeOut).toSeconds) { http =>
          val is: InputStream = http.getInputStream
          try is.copy(tmpVoCards)
          finally is.close
        tmpVoCards move voCards
      }
    }
    voCards
  }

  def getVOMS(vo: String): Option[String] = getVOMS(vo, xml.XML.loadFile(voCards))

  def getVOMS(vo: String, x: xml.Node) =  {
    import xml._

    def attributeIsDefined(name: String, value: String) =
      (_: Node).attribute(name).filter(_.text==value).isDefined

    val card = (x \ "IDCard" filter (attributeIsDefined("Name", vo))).headOption

     card map {
        card =>
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
}

trait GliteAuthentication {

  def apply(
    serverURL: String,
    voName: String,
    proxyFile: File,
    lifeTime: Int,
    fqan: Option[String]): GlobusGSSCredentialImpl

}

//class GliteAuthentication(
//  val voName: String,
//  val vomsURL: String,
//  val myProxy: Option[MyProxy],
//  val fqan: String) extends Authentication {
//
//  import GliteAuthentication.{ logger, addContext }
//
//  @transient private var _proxyExpiresTime = Long.MaxValue
//
//  val CACertificatesDir: File = GliteAuthentication.CACertificatesDir
//
//  override def key = "glite:" + (voName, vomsURL).toString
//
//  override def expires = _proxyExpiresTime
//
//  override def initialize(local: Boolean): Unit = {
//    if (!local) {
//      val globusProxy =
//        if (System.getenv.containsKey("X509_USER_PROXY") && new File(System.getenv.get("X509_USER_PROXY")).exists) System.getenv.get("X509_USER_PROXY")
//      else throw new InternalProcessingError("The X509_USER_PROXY environment variable is not defined or point to an inexisting file.")
//      myProxy match {
//        case Some(myProxy) ⇒ {
//            val ctx = JSAGASessionService.createContext
//            ctx.setAttribute(Context.TYPE, "VOMSMyProxy")
//            ctx.setAttribute(Context.USERPROXY, globusProxy)
//            ctx.setAttribute(Context.CERTREPOSITORY, CACertificatesDir.getCanonicalPath)
//            ctx.setAttribute(VOMSContext.MYPROXYUSERID, myProxy.userId)
//            ctx.setAttribute(VOMSContext.MYPROXYPASS, myProxy.pass)
//            ctx.setAttribute(VOMSContext.MYPROXYSERVER, myProxy.url)
//            ctx.setAttribute(VOMSContext.DELEGATIONLIFETIME, GliteAuthentication.getTimeString)
//            ctx.setAttribute(VOMSContext.VOMSDIR, "")
//            init(ctx, false)
//          }
//        case None ⇒
//          val (ctx, expires) = new GlobusProxyFile(globusProxy).init(this)
//          init(ctx, expires)
//      }
//    } else {
//      val auth = Workspace.persistentList(classOf[GliteAuthenticationMethod]).headOption match {
//        case Some((i, a)) ⇒ a
//        case None ⇒ throw new UserBadDataError("Preferences not set for grid authentication")
//      }
//
//      val (ctx, expires) = auth.init(this)
//      init(ctx, expires)
//    }
//
//  }
//
//  def reinit(context: Context, expires: Boolean) = {
//    addContext(context)
//    if (expires) _proxyExpiresTime = System.currentTimeMillis + context.getAttribute(Context.LIFETIME).toLong * 1000
//  }
//
//  def init(context: Context, expires: Boolean) = {
//    reinit(context, expires)
//    Updater.delay(new ProxyChecker(context, new WeakReference(this), expires))
//  }
//}
