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
import org.openmole.plugin.environment.glite.internal.Activator
import org.openmole.plugin.environment.glite.internal.BDII
import org.openmole.plugin.environment.glite.internal.DicotomicWorkloadStrategy
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
  val OverSubmissionIntervalLocation = new ConfigurationLocation("GliteEnvironment", "OverSubmissionInterval")
  val OverSubmissionRatioWaitingLocation = new ConfigurationLocation("GliteEnvironment", "OverSubmissionRatioWaiting")
  val OverSubmissionRatioRunningLocation = new ConfigurationLocation("GliteEnvironment", "OverSubmissionRatioRunning")
  val OverSubmissionMinJob = new ConfigurationLocation("GliteEnvironment", "OverSubmissionMinJob")
  val OverSubmissionNumberOfJobUnderMin = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNumberOfJobUnderMin")
  val OverSubmissionRatioEpsilonLocation = new ConfigurationLocation("GliteEnvironment", "OverSubmissionRatioEpsilon")
  val LocalThreadsBySELocation = new ConfigurationLocation("GliteEnvironment", "LocalThreadsBySE")
  val LocalThreadsByWMSLocation = new ConfigurationLocation("GliteEnvironment", "LocalThreadsByWMS")
  val ProxyRenewalRatio = new ConfigurationLocation("GliteEnvironment", "ProxyRenewalRatio")
  val JobShakingInterval = new ConfigurationLocation("GliteEnvironment", "JobShakingInterval")
  val JobShakingProbability = new ConfigurationLocation("GliteEnvironment", "JobShakingProbability")

  Activator.getWorkspace += (CertificatePathLocation, () => System.getProperty("user.home") + "/.globus/usercert.pem")

  Activator.getWorkspace += (KeyPathLocation, () => System.getProperty("user.home") + "/.globus/userkey.pem")

  Activator.getWorkspace += (P12CertificateLocation, () => System.getProperty("user.home") + "/.globus/certificate.p12")

  Activator.getWorkspace += (CertificateType, "pem")
  Activator.getWorkspace += (TimeLocation, "PT24H")
  Activator.getWorkspace += (DelegationTimeLocation, "P7D")

  Activator.getWorkspace += (FetchRessourcesTimeOutLocation, "PT2M")
  Activator.getWorkspace += (CACertificatesSiteLocation, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/")

  Activator.getWorkspace += (LocalThreadsBySELocation, "10")
  Activator.getWorkspace += (LocalThreadsByWMSLocation, "10")

  Activator.getWorkspace += (ProxyRenewalRatio, "0.2")

  Activator.getWorkspace += (OverSubmissionRatioWaitingLocation, "0.5")
  Activator.getWorkspace += (OverSubmissionRatioRunningLocation, "0.2")
  Activator.getWorkspace += (OverSubmissionRatioEpsilonLocation, "0.01")
  Activator.getWorkspace += (OverSubmissionIntervalLocation, "PT5M")

  Activator.getWorkspace += (OverSubmissionMinJob, "100")
  Activator.getWorkspace += (OverSubmissionNumberOfJobUnderMin, "3")
  
  Activator.getWorkspace += (JobShakingInterval, "PT5M")
  Activator.getWorkspace += (JobShakingProbability, "0.02")
}

class GliteEnvironment(val voName: String, val vomsURL: String, val bdii: String, val myProxy: Option[MyProxy], attributes: Option[Map[String, String]], memoryForRuntime: Option[Int], val fqan: String = "") extends JSAGAEnvironment(attributes, memoryForRuntime) {

  import GliteEnvironment._

  val threadsBySE = Activator.getWorkspace.preferenceAsInt(LocalThreadsBySELocation)
  val threadsByWMS = Activator.getWorkspace.preferenceAsInt(LocalThreadsByWMSLocation)
  val overSubmissionWaitingRatio = Activator.getWorkspace.preferenceAsDouble(OverSubmissionRatioWaitingLocation)
  val overSubmissionRunningRatio = Activator.getWorkspace.preferenceAsDouble(OverSubmissionRatioRunningLocation)
  val overSubmissionEpsilonRatio = Activator.getWorkspace.preferenceAsDouble(OverSubmissionRatioEpsilonLocation)
  val overSubmissionInterval = Activator.getWorkspace.preferenceAsDurationInMs(OverSubmissionIntervalLocation)
  val minJobs = Activator.getWorkspace.preferenceAsInt(OverSubmissionMinJob)
  val numberOfJobUnderMin = Activator.getWorkspace.preferenceAsInt(OverSubmissionNumberOfJobUnderMin)
       
  Activator.getUpdater.registerForUpdate(new OverSubmissionAgent(this, DicotomicWorkloadStrategy(overSubmissionWaitingRatio, overSubmissionRunningRatio, overSubmissionEpsilonRatio), minJobs, numberOfJobUnderMin), ExecutorType.OWN, overSubmissionInterval)
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
    val jss = getBDII.queryWMSURIs(voName, Activator.getWorkspace.preferenceAsDurationInMs(FetchRessourcesTimeOutLocation).toInt)

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
    val stors = getBDII.querySRMURIs(voName, Activator.getWorkspace.preferenceAsDurationInMs(GliteEnvironment.FetchRessourcesTimeOutLocation).toInt);

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
