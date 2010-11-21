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

import fr.in2p3.jsaga.adaptor.base.usage.UDuration
import fr.in2p3.jsaga.adaptor.security.VOMSContext
import fr.in2p3.jsaga.generated.parser.ParseException
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.ogf.saga.context.Context
import org.openmole.commons.aspect.caching.Cachable
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.tools.io.FileOutputStream
import org.openmole.commons.tools.io.FileUtil
import org.openmole.core.file.URIFile
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.plugin.environment.glite.internal.Activator
import org.openmole.plugin.environment.glite.internal.ProxyChecker
import org.openmole.plugin.environment.glite.internal.ProxyChecker

import scala.collection.JavaConversions._

object GliteAuthentication {
  def getCertType: String = {
    Activator.getWorkspace.getPreference(GliteEnvironment.CertificateType)
  }

  def getP12CertPath: String = {
    Activator.getWorkspace().getPreference(GliteEnvironment.P12CertificateLocation)
  }

  def getCertPath: String = {
    Activator.getWorkspace().getPreference(GliteEnvironment.CertificatePathLocation)
  }

  def getKeyPath: String = {
    Activator.getWorkspace().getPreference(GliteEnvironment.KeyPathLocation)
  }

  def getTimeString: String = {
    Activator.getWorkspace().getPreference(GliteEnvironment.TimeLocation)
  }

  def getTime: Long = {
    try {
      return UDuration.toInt(getTimeString) * 1000L
    } catch {
      case (ex: ParseException) => throw new UserBadDataError(ex)
    }
  }

  def getDelegationTimeString: String = {
    Activator.getWorkspace().getPreference(GliteEnvironment.DelegationTimeLocation)
  }

  def dowloadCACertificates(uri: URI, dir: File) = {

    val site = new URIFile(uri)

    for (tarUrl <- site.list) {
      try {
        val child = site.child(tarUrl)
        val is = child.openInputStream

        val tis = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(is)))

        try {
          //Bypass the directory
          var tarEntry = tis.getNextTarEntry

          tarEntry = tis.getNextTarEntry

          while (tarEntry != null) {
            var dest = new File(dir, tarEntry.getName())
            dest = new File(dir, dest.getName())

            if (dest.exists) {
              dest.delete
            }

            val os = new FileOutputStream(dest);
            try {
              FileUtil.copy(tis, os)
            } finally {
              os.close
            }

            tarEntry = tis.getNextTarEntry
          }
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


class GliteAuthentication(voName: String, vomsURL: String, myProxy: Option[MyProxy], fqan: String) extends IBatchServiceAuthentication {

  import GliteAuthentication._
    
  @transient 
  private var proxy: File = null
    
  @transient 
  private var _proxyExpiresTime = Long.MaxValue
  
  def proxyExpiresTime = _proxyExpiresTime

  //FIXME lazy val in scala 2.9.0 
  //@transient lazy val 
  def CACertificatesDir: File = {
    val X509_CERT_DIR = System.getenv("X509_CERT_DIR")
            
    if (X509_CERT_DIR != null && new File(X509_CERT_DIR).exists){
      new File(X509_CERT_DIR)
    } else {
      val caDir = new File("/etc/grid-security/certificates/")
      if (caDir.exists) {
        caDir
      } else {
        val tmp = Activator.getWorkspace.getFile("CACertificates")

        if (!tmp.exists || !new File(tmp, ".complete").exists) {
          tmp.mkdir();
          dowloadCACertificates(new URI(Activator.getWorkspace().getPreference(GliteEnvironment.CACertificatesSiteLocation)), tmp);
          new File(tmp, ".complete").createNewFile
        }
        tmp
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
    val ctx = Activator.getJSagaSessionService.createContext
    val proxyDuration = initContext(ctx)

    val interval = (proxyDuration * Activator.getWorkspace().getPreferenceAsDouble(GliteEnvironment.ProxyRenewalRatio)).toLong

    Activator.getUpdater.delay(new ProxyChecker(this, ctx), ExecutorType.OWN, interval);
    Activator.getJSagaSessionService.addContext(ctx)
  }

  def initContext(ctx: Context): Long = {
    ctx.setAttribute(VOMSContext.VOMSDIR, "")
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
      proxy = Activator.getWorkspace().newFile("proxy", ".x509")

      if (getCertType.equalsIgnoreCase("p12")) {
        ctx.setAttribute(VOMSContext.USERCERTKEY, getP12CertPath)
      } else if (getCertType.equalsIgnoreCase("pem")) {
        ctx.setAttribute(Context.USERCERT, getCertPath)
        ctx.setAttribute(Context.USERKEY, getKeyPath)
      } else {
        throw new UserBadDataError("Unknown certificate type " + getCertType)
      }

      val keyPassword = {
        val pass = Activator.getWorkspace.getPreference(GliteEnvironment.PasswordLocation)
        if(pass == null) "" else pass
      }

      ctx.setAttribute(Context.USERPASS, keyPassword)

      if (!fqan.isEmpty)  ctx.setAttribute(VOMSContext.USERFQAN, fqan)
      
    }

    ctx.setAttribute(Context.USERPROXY, proxy.getAbsolutePath)
    ctx.setAttribute(Context.SERVER, vomsURL)
    ctx.setAttribute(Context.USERVO, voName)

    ctx.getAttribute(Context.USERID)
 
    proxyDuration

  }

  private def createContextFromFile(proxyFile: File) = {
    val ctx = Activator.getJSagaSessionService.createContext
    ctx.setAttribute(Context.USERPROXY, proxyFile.getCanonicalPath)
    ctx.setAttribute(Context.CERTREPOSITORY, CACertificatesDir.getCanonicalPath)
    ctx.setAttribute(VOMSContext.VOMSDIR, "")
    ctx.setAttribute(Context.TYPE, "GlobusLegacy")

    Activator.getJSagaSessionService.addContext(ctx)
  }

}
