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


import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.InteractiveConfiguration
import java.net.URI
import java.net.URISyntaxException
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.core.batch.environment.BatchStorage
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.plugin.environment.glite.internal.Activator._
import org.openmole.plugin.environment.glite.internal.BDII
import org.openmole.plugin.environment.glite.internal.OverSubmissionAgent
import org.openmole.plugin.environment.jsaga.JSAGAEnvironment
import org.openmole.plugin.environment.jsaga.JSAGAJobService

import scala.annotation.target.field
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object GliteEnvironment {

  @field @InteractiveConfiguration(label = "CertificateType type", choices = Array("pem", "p12"))
  val CertificateType = new ConfigurationLocation("GliteEnvironment", "CertificateType")

  @field @InteractiveConfiguration(label = "PEM Certificate location", dependOn = "CertificateType", value = "pem")
  val CertificatePathLocation = new ConfigurationLocation("GliteEnvironment", "CertificatePath")

  @field @InteractiveConfiguration(label = "PEM Key location", dependOn = "CertificateType", value = "pem")
  val KeyPathLocation = new ConfigurationLocation("GliteEnvironment", "KeyPath")

  @field @InteractiveConfiguration(label = "P12 Certificate Location", dependOn = "CertificateType", value = "p12")
  val P12CertificateLocation = new ConfigurationLocation("GliteEnvironment", "P12CertificateLocation")

  @field @InteractiveConfiguration(label = "Key password")
  val PasswordLocation = new ConfigurationLocation("GliteEnvironment", "Password", true)
       
  val TimeLocation = new ConfigurationLocation("GliteEnvironment", "Time")
  val DelegationTimeLocation = new ConfigurationLocation("GliteEnvironment", "DelegationTime")

  val FetchRessourcesTimeOutLocation = new ConfigurationLocation("GliteEnvironment", "FetchRessourcesTimeOut")
  val CACertificatesSiteLocation = new ConfigurationLocation("GliteEnvironment", "CACertificatesSite")

  val OverSubmissionInterval = new ConfigurationLocation("GliteEnvironment", "OverSubmissionInterval")
  val OverSubmissionMinNumberOfJob = new ConfigurationLocation("GliteEnvironment", "OverSubmissionMinNumberOfJob")
  val OverSubmissionNumberOfJobUnderMin = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNumberOfJobUnderMin")
  val OverSubmissionNbSampling = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNbSampling")
  val OverSubmissionGridSizeRatio = new ConfigurationLocation("GliteEnvironment", "OverSubmissionGridSizeRatio")
  val OverSubmissionSamplingWindowFactor = new ConfigurationLocation("GliteEnvironment", "OverSubmissionSamplingWindowFactor")


  val LocalThreadsBySELocation = new ConfigurationLocation("GliteEnvironment", "LocalThreadsBySE")
  val LocalThreadsByWMSLocation = new ConfigurationLocation("GliteEnvironment", "LocalThreadsByWMS")
  val ProxyRenewalRatio = new ConfigurationLocation("GliteEnvironment", "ProxyRenewalRatio")
  val JobShakingInterval = new ConfigurationLocation("GliteEnvironment", "JobShakingInterval")
  val JobShakingProbabilitySubmitted = new ConfigurationLocation("GliteEnvironment", "JobShakingProbabilitySubmitted")
  val JobShakingProbabilityQueued = new ConfigurationLocation("GliteEnvironment", "JobShakingProbabilityQueued")

  workspace += (CertificatePathLocation, () => System.getProperty("user.home") + "/.globus/usercert.pem")

  workspace += (KeyPathLocation, () => System.getProperty("user.home") + "/.globus/userkey.pem")

  workspace += (P12CertificateLocation, () => System.getProperty("user.home") + "/.globus/certificate.p12")

  workspace += (CertificateType, "pem")
  workspace += (TimeLocation, "PT24H")
  workspace += (DelegationTimeLocation, "P7D")

  workspace += (FetchRessourcesTimeOutLocation, "PT2M")
  workspace += (CACertificatesSiteLocation, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/")

  workspace += (LocalThreadsBySELocation, "10")
  workspace += (LocalThreadsByWMSLocation, "10")

  workspace += (ProxyRenewalRatio, "0.2")

  workspace += (OverSubmissionNbSampling, "10")
  workspace += (OverSubmissionSamplingWindowFactor, "5")
  
 // workspace += (OverSubmissionGridSizeRatio, "0.25")
  workspace += (OverSubmissionInterval, "PT5M")

  workspace += (OverSubmissionMinNumberOfJob, "100")
  workspace += (OverSubmissionNumberOfJobUnderMin, "3")
  
  workspace += (JobShakingInterval, "PT5M")
  workspace += (JobShakingProbabilitySubmitted, "0.1")
  workspace += (JobShakingProbabilityQueued, "0.01")
 
}

class GliteEnvironment(val voName: String, val vomsURL: String, val bdii: String, val myProxy: Option[MyProxy], attributes: Option[Map[String, String]], memoryForRuntime: Option[Int], val fqan: String = "") extends JSAGAEnvironment(attributes, memoryForRuntime) {

  import GliteEnvironment._

  val threadsBySE = workspace.preferenceAsInt(LocalThreadsBySELocation)
  val threadsByWMS = workspace.preferenceAsInt(LocalThreadsByWMSLocation)
       
  updater.registerForUpdate(new OverSubmissionAgent(this), ExecutorType.OWN)
 // Activator.getUpdater.registerForUpdate(new JobShaker(this, Activator.getWorkspace.preferenceAsDouble(JobShakingProbability)), ExecutorType.OWN, Activator.getWorkspace.preferenceAsDurationInMs(JobShakingInterval))
  
  def this(voName: String, vomsURL: String, bdii: String) = this(voName, vomsURL, bdii, None, None, None)

  def this(voName: String, vomsURL: String, bdii: String, attributes: java.util.Map[String, String]) = this(voName, vomsURL, bdii, None, Some(attributes.toMap), None)
 
  def this(voName: String, vomsURL: String, bdii: String, memoryForRuntime: Int, attributes: java.util.Map[String, String]) = this(voName, vomsURL, bdii, None, Some(attributes.toMap), Some(memoryForRuntime))
  
  def this(voName: String, vomsURL: String, bdii: String, myProxy: String, myProxyUserId: String, myProxyPass: String) =this(voName, vomsURL, bdii, Some(new MyProxy(myProxy, myProxyUserId,myProxyPass)), None, None)

  def this(voName: String, vomsURL: String, bdii: String, myProxy: String, myProxyUserId: String, myProxyPass: String, attributes: java.util.Map[String, String]) = this(voName, vomsURL, bdii, Some(new MyProxy(myProxy, myProxyUserId,myProxyPass)), Some(attributes.toMap), None)
 
  def this(voName: String, vomsURL: String, bdii: String, myProxy: String, myProxyUserId: String, myProxyPass: String, memoryForRuntime: Int, attributes: java.util.Map[String, String]) =this(voName, vomsURL, bdii, Some(new MyProxy(myProxy, myProxyUserId,myProxyPass)), Some(attributes.toMap), Some(memoryForRuntime))
  
  def this(voName: String, vomsURL: String, bdii: String, fqan: String) = this(voName, vomsURL, bdii, None, None, None)

  def this(voName: String, vomsURL: String, bdii: String, fqan: String, attributes: java.util.Map[String, String]) = this(voName, vomsURL, bdii, None, Some(attributes.toMap), None, fqan)
 
  def this(voName: String, vomsURL: String, bdii: String, fqan: String, memoryForRuntime: Int, attributes: java.util.Map[String, String]) = this(voName, vomsURL, bdii, None, Some(attributes.toMap), Some(memoryForRuntime), fqan)
  
  def this(voName: String, vomsURL: String, bdii: String, fqan: String, myProxy: String, myProxyUserId: String, myProxyPass: String) =this(voName, vomsURL, bdii, Some(new MyProxy(myProxy, myProxyUserId,myProxyPass)), None, None, fqan)

  def this(voName: String, vomsURL: String, bdii: String, fqan: String, myProxy: String, myProxyUserId: String, myProxyPass: String, attributes: java.util.Map[String, String]) = this(voName, vomsURL, bdii, Some(new MyProxy(myProxy, myProxyUserId,myProxyPass)), Some(attributes.toMap), None, fqan)
 
  def this(voName: String, vomsURL: String, bdii: String, fqan: String, myProxy: String, myProxyUserId: String, myProxyPass: String, memoryForRuntime: Int, attributes: java.util.Map[String, String]) =this(voName, vomsURL, bdii, Some(new MyProxy(myProxy, myProxyUserId,myProxyPass)), Some(attributes.toMap), Some(memoryForRuntime), fqan)
  
  override def allJobServices: Iterable[GliteJobService] = {
    val jss = getBDII.queryWMSURIs(voName, workspace.preferenceAsDurationInMs(FetchRessourcesTimeOutLocation).toInt)

    val jobServices = new ListBuffer[GliteJobService]

    for (js <- jss) {
      try {
        val wms = new URI("wms:" + js.getRawSchemeSpecificPart)

        val jobService = new GliteJobService(wms, this, authenticationKey, authentication, threadsByWMS)
        jobServices += jobService
      } catch {
        case (e: URISyntaxException) => Logger.getLogger(GliteEnvironment.getClass.getName).log(Level.WARNING, "wms:" + js.getRawSchemeSpecificPart(), e);
      }
    }
    jobServices.toList
  }

  override def allStorages: Iterable[BatchStorage] = {
    val allStorages = new ListBuffer[BatchStorage]
    val stors = getBDII.querySRMURIs(voName, workspace.preferenceAsDurationInMs(GliteEnvironment.FetchRessourcesTimeOutLocation).toInt);

    for (stor <- stors) {
      val storage = new BatchStorage(stor, authenticationKey, authentication,threadsBySE)
      allStorages += storage
    }

    allStorages
  }

  @transient lazy val authentication = new GliteAuthentication(voName, vomsURL, myProxy, fqan)
  @transient lazy val authenticationKey = new GliteAuthenticationKey(voName, vomsURL)

  
  private def getBDII: BDII = {
    new BDII(bdii)
  }
}
