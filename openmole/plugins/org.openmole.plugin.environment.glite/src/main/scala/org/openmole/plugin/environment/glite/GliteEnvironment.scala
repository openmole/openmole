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

import org.openmole.misc.filedeleter.FileDeleter
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.ConfigurationLocation
import fr.iscpif.gridscale.information.BDII
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.exception._
import org.openmole.plugin.environment.gridscale._
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl
import ref.WeakReference

object GliteEnvironment {

  val ProxyTime = new ConfigurationLocation("GliteEnvironment", "ProxyTime")
  val MyProxyTime = new ConfigurationLocation("GliteEnvironment", "MyProxyTime")

  val FetchRessourcesTimeOut = new ConfigurationLocation("GliteEnvironment", "FetchRessourcesTimeOut")
  val CACertificatesSite = new ConfigurationLocation("GliteEnvironment", "CACertificatesSite")

  val OverSubmissionInterval = new ConfigurationLocation("GliteEnvironment", "OverSubmissionInterval")
  val OverSubmissionMinNumberOfJob = new ConfigurationLocation("GliteEnvironment", "OverSubmissionMinNumberOfJob")
  val OverSubmissionNumberOfJobUnderMin = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNumberOfJobUnderMin")
  val OverSubmissionNbSampling = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNbSampling")
  val OverSubmissionSamplingWindowFactor = new ConfigurationLocation("GliteEnvironment", "OverSubmissionSamplingWindowFactor")

  val LocalThreadsBySE = new ConfigurationLocation("GliteEnvironment", "LocalThreadsBySE")
  val LocalThreadsByWMS = new ConfigurationLocation("GliteEnvironment", "LocalThreadsByWMS")
  val ProxyRenewalRatio = new ConfigurationLocation("GliteEnvironment", "ProxyRenewalRatio")
  val MinProxyRenewal = new ConfigurationLocation("GliteEnvironment", "MinProxyRenewal")
  val JobShakingHalfLife = new ConfigurationLocation("GliteEnvironment", "JobShakingHalfLife")
  val JobShakingMaxReady = new ConfigurationLocation("GliteEnvironment", "JobShakingMaxReady")

  val LCGCPTimeOut = new ConfigurationLocation("GliteEnvironment", "RuntimeCopyOnWNTimeOut")

  Workspace += (ProxyTime, "PT24H")
  Workspace += (MyProxyTime, "P7D")

  Workspace += (FetchRessourcesTimeOut, "PT5M")
  Workspace += (CACertificatesSite, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/")

  Workspace += (LocalThreadsBySE, "10")
  Workspace += (LocalThreadsByWMS, "10")

  Workspace += (ProxyRenewalRatio, "0.2")

  Workspace += (MinProxyRenewal, "PT5M")

  Workspace += (OverSubmissionNbSampling, "10")
  Workspace += (OverSubmissionSamplingWindowFactor, "5")

  Workspace += (OverSubmissionInterval, "PT2M")

  Workspace += (OverSubmissionMinNumberOfJob, "100")
  Workspace += (OverSubmissionNumberOfJobUnderMin, "10")

  Workspace += (JobShakingHalfLife, "PT30M")
  Workspace += (JobShakingMaxReady, "100")

  Workspace += (LCGCPTimeOut, "PT5M")
}

class GliteEnvironment(
    val voName: String,
    val vomsURL: String,
    val bdii: String,
    val fqan: Option[String] = None,
    override val runtimeMemory: Option[Int] = None) extends BatchEnvironment { env ⇒

  import GliteEnvironment._

  val id = voName + "@" + vomsURL
  val threadsBySE = Workspace.preferenceAsInt(LocalThreadsBySE)
  val threadsByWMS = Workspace.preferenceAsInt(LocalThreadsByWMS)

  type JS = GliteJobService
  type SS = PersistentStorageService

  Updater.registerForUpdate(new OverSubmissionAgent(WeakReference(this)))
  Updater.registerForUpdate(new ProxyChecker(WeakReference(this)))

  private def generateProxy = Workspace.persistentList(classOf[GliteAuthentication]).headOption match {
    case Some(a) ⇒
      val file = Workspace.newFile("proxy", ".x509")
      FileDeleter.deleteWhenGarbageCollected(file)
      val proxy = a._2.apply(
        vomsURL,
        voName,
        file,
        Workspace.preferenceAsDurationInS(ProxyTime),
        fqan)
      (proxy, file)
    case None ⇒ throw new UserBadDataError("No athentication has been initialized for glite.")
  }

  var authentication = generateProxy

  def renewAuthentication = synchronized {
    authentication = generateProxy
    jobServices.foreach { j ⇒ j.proxyFile = authentication._2; j.delegated = false }
  }

  override def allJobServices = {
    val jss = getBDII.queryWMS(voName, Workspace.preferenceAsDurationInS(FetchRessourcesTimeOut).toInt)
    jss.map {
      js ⇒ new GliteJobService(js, this, threadsByWMS, authentication._2)
    }
  }

  override def allStorages = {
    val stors = getBDII.querySRM(voName, Workspace.preferenceAsDurationInS(GliteEnvironment.FetchRessourcesTimeOut).toInt)
    stors.map {
      s ⇒ GliteStorageService(s, env, GliteAuthentication.CACertificatesDir)
    }
  }

  private def getBDII: BDII = new BDII(bdii)

}
