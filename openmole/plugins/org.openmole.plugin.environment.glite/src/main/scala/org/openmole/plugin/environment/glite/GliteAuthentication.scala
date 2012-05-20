/*
 * Copyright (C) 2010 reuillon
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
import fr.in2p3.jsaga.adaptor.base.usage.UDuration
import fr.in2p3.jsaga.adaptor.security.VOMSContext

import fr.in2p3.jsaga.generated.parser.ParseException
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import org.openmole.misc.tools.service.Logger
import java.nio.file.FileSystems
import java.util.zip.GZIPInputStream
import org.ogf.saga.context.Context
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.environment.Authentication
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.jsaga.JSAGASessionService
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.Workspace
import GliteEnvironment._

import scala.collection.JavaConversions._
import scala.ref.WeakReference

object GliteAuthentication extends Logger {

  lazy val CACertificatesDir: File = {
    /*val X509_CERT_DIR = System.getenv("X509_CERT_DIR")

     if (X509_CERT_DIR != null && new File(X509_CERT_DIR).exists) new File(X509_CERT_DIR)
     else {
     val caDir = new File("/etc/grid-security/certificates/")
     if (caDir.exists) caDir
     else {*/
    val caDir = Workspace.file("CACertificates")

    if (!caDir.exists || !new File(caDir, ".complete").exists) {
      caDir.mkdir
      dowloadCACertificates(new URI(Workspace.preference(GliteEnvironment.CACertificatesSite)), caDir)
      new File(caDir, ".complete").createNewFile
    }
    caDir
    /*  }
     }*/
  }

  def dowloadCACertificates(uri: URI, dir: File) = {
    val fs = FileSystems.getDefault

    val site = new URIFile(uri)

    for (tarUrl ← site.list) {
      try {
        val child = site.child(tarUrl)
        val is = child.openInputStream

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
          case (e: IOException) ⇒ logger.log(WARNING, "Unable to untar " + child.toString(), e)
        } finally tis.close
      } catch {
        case (e: IOException) ⇒ throw new IOException(tarUrl, e)
      }
    }
  }

  def addContext(ctx: Context) {
    JSAGASessionService.addContext("wms://.*", ctx)
    JSAGASessionService.addContext("srm://.*", ctx)
  }

  def getTimeString: String = Workspace.preference(GliteEnvironment.ProxyTime)

  def inMs(time: String) =
    try UDuration.toInt(time) * 1000
  catch {
    case (ex: ParseException) ⇒ throw new UserBadDataError(ex)
  }

}

class GliteAuthentication(
  val voName: String,
  val vomsURL: String,
  val myProxy: Option[MyProxy],
  val fqan: String) extends Authentication {

  import GliteAuthentication.{ logger, addContext }

  @transient private var _proxyExpiresTime = Long.MaxValue

  val CACertificatesDir: File = GliteAuthentication.CACertificatesDir

  override def key = "glite:" + (voName, vomsURL).toString

  override def expires = _proxyExpiresTime

  override def initialize(local: Boolean): Unit = {
    if (!local) {
      val globusProxy =
        if (System.getenv.containsKey("X509_USER_PROXY") && new File(System.getenv.get("X509_USER_PROXY")).exists) System.getenv.get("X509_USER_PROXY")
      else throw new InternalProcessingError("The X509_USER_PROXY environment variable is not defined or point to an inexisting file.")
      myProxy match {
        case Some(myProxy) ⇒ {
            val ctx = JSAGASessionService.createContext
            ctx.setAttribute(Context.TYPE, "VOMSMyProxy")
            ctx.setAttribute(Context.USERPROXY, globusProxy)
            ctx.setAttribute(Context.CERTREPOSITORY, CACertificatesDir.getCanonicalPath)
            ctx.setAttribute(VOMSContext.MYPROXYUSERID, myProxy.userId)
            ctx.setAttribute(VOMSContext.MYPROXYPASS, myProxy.pass)
            ctx.setAttribute(VOMSContext.MYPROXYSERVER, myProxy.url)
            ctx.setAttribute(VOMSContext.DELEGATIONLIFETIME, myProxy.delegationTime)
            ctx.setAttribute(VOMSContext.VOMSDIR, "")
            init(ctx, false)
          }
        case None ⇒
          val (ctx, expires) = new GlobusProxyFile(globusProxy).init(this)
          init(ctx, expires)
      }
    } else {
      val auth = Workspace.persistentList(classOf[GliteAuthenticationMethod]).headOption match {
        case Some((i, a)) ⇒ a
        case None ⇒ throw new UserBadDataError("Preferences not set for grid authentication")
      }

      val (ctx, expires) = auth.init(this) 
      init(ctx, expires)
    }

  }

  def reinit(context: Context, expires: Boolean) = {
    addContext(context)
    if (expires) _proxyExpiresTime = System.currentTimeMillis + context.getAttribute(Context.LIFETIME).toLong * 1000
    logger.fine("Reinit proxy " + context.getAttribute(Context.TYPE) + ", life time " + context.getAttribute(Context.LIFETIME))
  }

  def init(context: Context, expires: Boolean) = {
    reinit(context, expires)
    Updater.delay(new ProxyChecker(context, new WeakReference(this), expires))
  }
}
