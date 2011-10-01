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

package org.openmole.plugin.environment.glite.authentication

import com.ice.tar.TarInputStream
import fr.in2p3.jsaga.adaptor.security.VOMSContext

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import org.openmole.misc.tools.service.Logger
import java.util.zip.GZIPInputStream
import org.ogf.saga.context.Context
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.environment.Authentication
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.jsaga.JSAGASessionService
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.glite.MyProxy
import org.openmole.plugin.environment.glite.GliteEnvironment
import org.openmole.plugin.environment.glite.GliteEnvironment._

import scala.collection.JavaConversions._

object GliteAuthentication extends Logger {
  
  lazy val CACertificatesDir: File = {
    val X509_CERT_DIR = System.getenv("X509_CERT_DIR")
            
    if (X509_CERT_DIR != null && new File(X509_CERT_DIR).exists){
      new File(X509_CERT_DIR)
    } else {
      val caDir = new File("/etc/grid-security/certificates/")
      if (caDir.exists) caDir
      else {
        val caDir = Workspace.file("CACertificates")

        if (!caDir.exists || !new File(caDir, ".complete").exists) {
          caDir.mkdir
          dowloadCACertificates(new URI(Workspace.preference(GliteEnvironment.CACertificatesSite)), caDir)
          new File(caDir, ".complete").createNewFile
        }
        caDir
      }
    }
  }

  def dowloadCACertificates(uri: URI, dir: File) = {

    val site = new URIFile(uri)

    for (tarUrl <- site.list) {
      try {
        val child = site.child(tarUrl)
        val is = child.openInputStream

        val tis = new TarInputStream(new GZIPInputStream(new BufferedInputStream(is)))

        try {
          //Bypass the directory
          var tarEntry = tis.getNextEntry
          tarEntry = tis.getNextEntry

          var links = List.empty[(File, String)]
          
          while (tarEntry != null) {
            val destForName = new File(dir, tarEntry.getName)
            val dest = new File(dir, destForName.getName)

            if (dest.exists) dest.delete
            if(!tarEntry.getLinkName.isEmpty) links ::= dest -> tarEntry.getLinkName
            else tis.copy(dest)
            
            tarEntry = tis.getNextEntry
          }
          
          links.foreach{e => new File(dir, e._2).copy(e._1)}
        } catch {
          case (e: IOException) => logger.log(WARNING, "Unable to untar " + child.toString(), e)
        } finally tis.close
      } catch {
        case (e: IOException) => throw new IOException(tarUrl, e)
      }
    }
  }
  
  def addContext(ctx: Context) {
    JSAGASessionService.addContext("wms://.*", ctx)
    JSAGASessionService.addContext("srm://.*", ctx)
  }
  
}


class GliteAuthentication(val voName: String, val vomsURL: String, val myProxy: Option[MyProxy], val fqan: String) extends Authentication {

  import GliteAuthentication._
    
  @transient
  private var proxy: File = null
    
  @transient private var _proxyExpiresTime = Long.MaxValue
  
  override def key = "glite:" + (voName, vomsURL).toString
  
  override def expires = _proxyExpiresTime

  override def initialize = {
    val authenticationMethod: GliteAuthenticationMethod = 
      if (System.getenv.containsKey("X509_USER_PROXY") && new File(System.getenv.get("X509_USER_PROXY")).exists) new GlobusProxyFile(System.getenv.get("X509_USER_PROXY"))
      else Workspace.persitentList(classOf[GliteAuthenticationMethod]).headOption match {
        case Some((i,a)) => a
        case None => getAuthenticationMethodFromPrefences
      }
    
    val (ctx, time) = authenticationMethod.init(this)
    time match {
      case Some(t) => 
        _proxyExpiresTime = System.currentTimeMillis + t
      case None =>
    }
    
    addContext(ctx)
  }
  
  def getAuthenticationMethodFromPrefences = Workspace.preference(CertificateType) match {
    case "pem" => new PEMCertificate(Workspace.rawPreference(PasswordLocation), 
                                     Workspace.preference(CertificatePathLocation), 
                                     Workspace.preference(KeyPathLocation))
    case "p12" => new P12Certificate(Workspace.rawPreference(PasswordLocation),
                                     Workspace.preference(P12CertificateLocation))
    case "proxy" => new VOMSProxyFile(Workspace.preference(ProxyLocation))
  }

}
