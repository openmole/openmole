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

import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.ConfigurationLocation
import java.net.URI
import java.net.URISyntaxException
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.ExecutionJobRegistry
import org.openmole.core.batch.environment.PersistentStorage
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga._

import scala.collection.JavaConversions._

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
  val JobShakingInterval = new ConfigurationLocation("GliteEnvironment", "JobShakingInterval")
  val JobShakingProbabilitySubmitted = new ConfigurationLocation("GliteEnvironment", "JobShakingProbabilitySubmitted")
  val JobShakingProbabilityQueued = new ConfigurationLocation("GliteEnvironment", "JobShakingProbabilityQueued")

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

  Workspace += (JobShakingInterval, "PT5M")
  Workspace += (JobShakingProbabilitySubmitted, "0.01")
  Workspace += (JobShakingProbabilityQueued, "0.01")

  Workspace += (LCGCPTimeOut, "PT5M")
}

class GliteEnvironment(
    val voName: String,
    val vomsURL: String,
    val bdii: String,
    val myProxy: Option[MyProxy] = None,
    val runtimeMemory: Int = BatchEnvironment.defaultRuntimeMemory,
    val requirements: Iterable[Requirement] = List.empty,
    val fqan: String = "") extends JSAGAEnvironment {

  import GliteEnvironment._

  val threadsBySE = Workspace.preferenceAsInt(LocalThreadsBySE)
  val threadsByWMS = Workspace.preferenceAsInt(LocalThreadsByWMS)

  Updater.registerForUpdate(new OverSubmissionAgent(this))

  override def allJobServices: Iterable[GliteJobService] = {
    val jss = getBDII.queryWMSURIs(voName, Workspace.preferenceAsDurationInMs(FetchRessourcesTimeOut).toInt)

    val jobServices = jss.flatMap {
      js ⇒
        try {
          val wms = new URI("wms:" + js.getRawSchemeSpecificPart)
          val jobService = new GliteJobService(wms, this, threadsByWMS)
          Some(jobService)
        } catch {
          case (e: URISyntaxException) ⇒
            Logger.getLogger(GliteEnvironment.getClass.getName).log(Level.WARNING, "wms:" + js.getRawSchemeSpecificPart(), e);
            None
        }
    }

    Logger.getLogger(classOf[GliteEnvironment].getName).fine(jobServices.toString)
    jobServices.toList
  }

  override def allStorages = {
    val stors = getBDII.querySRMURIs(voName, Workspace.preferenceAsDurationInMs(GliteEnvironment.FetchRessourcesTimeOut).toInt)
    stors.map { new PersistentStorage(this, _, threadsBySE) }
  }

  @transient lazy val authentication = new GliteAuthentication(voName, vomsURL, myProxy, fqan)

  private def getBDII: BDII = new BDII(bdii)

}
