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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import org.ogf.saga.context.Context
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.environment.BatchAuthentication
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.jsaga.JSAGASessionService
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.updater.internal.Updater
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.glite.internal.ProxyChecker
import org.openmole.plugin.environment.glite.internal.ProxyChecker

import scala.collection.JavaConversions._

object GliteAuthentication {
  def getCertType: String = Workspace.preference(GliteEnvironment.CertificateType)
  
  def getP12CertPath: String = Workspace.preference(GliteEnvironment.P12CertificateLocation)
 
  def getCertPath: String = Workspace.preference(GliteEnvironment.CertificatePathLocation)

  def getKeyPath: String = Workspace.preference(GliteEnvironment.KeyPathLocation)

  def getTimeString: String = Workspace.preference(GliteEnvironment.TimeLocation)
  
  def getTime: Long = {
    try {
      return UDuration.toInt(getTimeString) * 1000L
    } catch {
      case (ex: ParseException) => throw new UserBadDataError(ex)
    }
  }

  def getDelegationTimeString: String = Workspace.preference(GliteEnvironment.DelegationTimeLocation)
  

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
            //Logger.getLogger(classOf[GliteAuthentication].getName).fine(tarEntry.getName + " -> " + tarEntry.getLinkName)

            if(!tarEntry.getLinkName.isEmpty) links ::= dest -> tarEntry.getLinkName
            else tis.copy(dest)
            
            tarEntry = tis.getNextEntry
          }
          
          links.foreach{e => new File(dir, e._2).copy(e._1)}
        } catch {
          case (e: IOException) => Logger.getLogger(classOf[GliteAuthentication].getName()).log(Level.WARNING, "Unable to untar " + child.toString(), e);
        } finally {
          tis.close
        }
      } catch {
        case (e: IOException) => throw new IOException(tarUrl, e);
      }
    }
  }
}


class GliteAuthentication(voName: String, vomsURL: String, myProxy: Option[MyProxy], fqan: String) extends BatchAuthentication {

  import GliteAuthentication._
    
  @transient
  private var proxy: File = null
    
  @transient 
  private var _proxyExpiresTime = Long.MaxValue
  
  override def key = "glite:" + (voName, vomsURL).toString
  
  override def expires = _proxyExpiresTime

  @transient lazy val CACertificatesDir: File = {
    val X509_CERT_DIR = System.getenv("X509_CERT_DIR")
            
    if (X509_CERT_DIR != null && new File(X509_CERT_DIR).exists){
      new File(X509_CERT_DIR)
    } else {
      val caDir = new File("/etc/grid-security/certificates/")
      if (caDir.exists) {
        caDir
      } else {
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

  override def initialize = {
    if (System.getenv.containsKey("X509_USER_PROXY") && new File(System.getenv.get("X509_USER_PROXY")).exists) {
      createContextFromFile(new File(System.getenv.get("X509_USER_PROXY")))
    } else {
      createContextFromPreferences
    }
  }

  private def createContextFromPreferences = {
    val ctx = JSAGASessionService.createContext
    val proxyDuration = initContext(ctx)

    val interval = (proxyDuration * Workspace.preferenceAsDouble(GliteEnvironment.ProxyRenewalRatio)).toLong

    Updater.delay(new ProxyChecker(this, ctx), ExecutorType.OWN, interval)

  }

  def initContext(ctx: Context): Long = {
    ctx.setAttribute(VOMSContext.VOMSDIR, "")
  
    //Logger.getLogger(classOf[GliteAuthentication].getName).info(CACertificatesDir.getCanonicalPath)
    ctx.setAttribute(Context.CERTREPOSITORY, CACertificatesDir.getCanonicalPath)
  
    val proxyDuration = myProxy match {
      case Some(proxy) => 
        ctx.setAttribute(Context.TYPE, "VOMSMyProxy")
        ctx.setAttribute(VOMSContext.MYPROXYSERVER, proxy.url)
        ctx.setAttribute(VOMSContext.DELEGATIONLIFETIME, getDelegationTimeString)
        ctx.setAttribute(VOMSContext.MYPROXYUSERID,proxy.userId)
        ctx.setAttribute(VOMSContext.MYPROXYPASS, proxy.pass)
        _proxyExpiresTime = Long.MaxValue
        12 * 60 * 60 * 1000
      case None =>
        ctx.setAttribute(Context.TYPE, "VOMS")
        ctx.setAttribute(Context.LIFETIME, getTimeString)
        _proxyExpiresTime = System.currentTimeMillis + getTime
        getTime
    }
            
    if (proxy == null) {
      proxy = Workspace.newFile("proxy", ".x509")

      if (getCertType.equalsIgnoreCase("p12")) {
        ctx.setAttribute(VOMSContext.USERCERTKEY, getP12CertPath)
      } else if (getCertType.equalsIgnoreCase("pem")) {
        //Logger.getLogger(classOf[GliteAuthentication].getName).info(getCertPath)
        ctx.setAttribute(Context.USERCERT, getCertPath)
        //Logger.getLogger(classOf[GliteAuthentication].getName).info(getKeyPath)
        ctx.setAttribute(Context.USERKEY, getKeyPath)
      } else {
        throw new UserBadDataError("Unknown certificate type " + getCertType)
      }

      val keyPassword = {
        val pass = Workspace.preference(GliteEnvironment.PasswordLocation)
        //Logger.getLogger(classOf[GliteAuthentication].getName).info(pass)
        if(pass == null) "" else pass
      }

      ctx.setAttribute(Context.USERPASS, keyPassword)

      if (!fqan.isEmpty)  ctx.setAttribute(VOMSContext.USERFQAN, fqan)
      
    }
    //Logger.getLogger(classOf[GliteAuthentication].getName).info(proxy.getAbsolutePath)
    ctx.setAttribute(Context.USERPROXY, proxy.getAbsolutePath)
    // Logger.getLogger(classOf[GliteAuthentication].getName).info(vomsURL)
    ctx.setAttribute(Context.SERVER, vomsURL)
    
    //Logger.getLogger(classOf[GliteAuthentication].getName).info(voName)
    ctx.setAttribute(Context.USERVO, voName)

    JSAGASessionService.addContext(ctx)
    
    proxyDuration

  }

  private def createContextFromFile(proxyFile: File) = {
    val ctx = JSAGASessionService.createContext
    ctx.setAttribute(Context.USERPROXY, proxyFile.getCanonicalPath)
    ctx.setAttribute(Context.CERTREPOSITORY, CACertificatesDir.getCanonicalPath)
    ctx.setAttribute(VOMSContext.VOMSDIR, "")
    ctx.setAttribute(Context.TYPE, "GlobusLegacy")

    JSAGASessionService.addContext(ctx)
  }

}
