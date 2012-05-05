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

  val TimeLocation = new ConfigurationLocation("GliteEnvironment", "Time")

  val FetchRessourcesTimeOutLocation = new ConfigurationLocation("GliteEnvironment", "FetchRessourcesTimeOut")
  val CACertificatesSite = new ConfigurationLocation("GliteEnvironment", "CACertificatesSite")

  val OverSubmissionInterval = new ConfigurationLocation("GliteEnvironment", "OverSubmissionInterval")
  val OverSubmissionMinNumberOfJob = new ConfigurationLocation("GliteEnvironment", "OverSubmissionMinNumberOfJob")
  val OverSubmissionNumberOfJobUnderMin = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNumberOfJobUnderMin")
  val OverSubmissionNbSampling = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNbSampling")
  //val OverSubmissionGridSizeRatio = new ConfigurationLocation("GliteEnvironment", "OverSubmissionGridSizeRatio")
  val OverSubmissionSamplingWindowFactor = new ConfigurationLocation("GliteEnvironment", "OverSubmissionSamplingWindowFactor")

  val LocalThreadsBySELocation = new ConfigurationLocation("GliteEnvironment", "LocalThreadsBySE")
  val LocalThreadsByWMSLocation = new ConfigurationLocation("GliteEnvironment", "LocalThreadsByWMS")
  val ProxyRenewalRatio = new ConfigurationLocation("GliteEnvironment", "ProxyRenewalRatio")
  val JobShakingInterval = new ConfigurationLocation("GliteEnvironment", "JobShakingInterval")
  val JobShakingProbabilitySubmitted = new ConfigurationLocation("GliteEnvironment", "JobShakingProbabilitySubmitted")
  val JobShakingProbabilityQueued = new ConfigurationLocation("GliteEnvironment", "JobShakingProbabilityQueued")

  Workspace += (TimeLocation, "PT24H")

  Workspace += (FetchRessourcesTimeOutLocation, "PT5M")
  Workspace += (CACertificatesSite, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/")

  Workspace += (LocalThreadsBySELocation, "10")
  Workspace += (LocalThreadsByWMSLocation, "10")

  Workspace += (ProxyRenewalRatio, "0.2")

  Workspace += (OverSubmissionNbSampling, "10")
  Workspace += (OverSubmissionSamplingWindowFactor, "5")

  // Workspace += (OverSubmissionGridSizeRatio, "0.25")
  Workspace += (OverSubmissionInterval, "PT2M")

  Workspace += (OverSubmissionMinNumberOfJob, "100")
  Workspace += (OverSubmissionNumberOfJobUnderMin, "5")

  Workspace += (JobShakingInterval, "PT5M")
  Workspace += (JobShakingProbabilitySubmitted, "0.1")
  Workspace += (JobShakingProbabilityQueued, "0.01")
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

  val threadsBySE = Workspace.preferenceAsInt(LocalThreadsBySELocation)
  val threadsByWMS = Workspace.preferenceAsInt(LocalThreadsByWMSLocation)

  Updater.registerForUpdate(new OverSubmissionAgent(this))

  override def allJobServices: Iterable[GliteJobService] = {
    val jss = getBDII.queryWMSURIs(voName, Workspace.preferenceAsDurationInMs(FetchRessourcesTimeOutLocation).toInt)

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
    val stors = getBDII.querySRMURIs(voName, Workspace.preferenceAsDurationInMs(GliteEnvironment.FetchRessourcesTimeOutLocation).toInt)
    stors.map { new PersistentStorage(this, _, threadsBySE) }
  }

  @transient lazy val authentication = new GliteAuthentication(voName, vomsURL, myProxy, fqan)

  private def getBDII: BDII = new BDII(bdii)

}
