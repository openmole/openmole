///*
// * Copyright (C) 10/06/13 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package org.openmole.plugin.environment.egi
//
//import java.io.File
//import java.net.URI
//
//import fr.iscpif.gridscale.egi.BDII
//import org.openmole.core.preference.{ ConfigurationLocation, Preference }
//import org.openmole.core.threadprovider.Updater
//import squants._
//import org.openmole.core.workflow.dsl._
//import org.openmole.core.workflow.job.Job
//import org.openmole.core.workspace.Workspace
//import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, UpdateInterval }
//import org.openmole.tool.cache.Cache
//import org.openmole.tool.crypto.Cypher
//import squants.information._
//import squants.time.TimeConversions._
//
//import scala.ref.WeakReference
//
//object DIRACEnvironment {
//
//  val EagerSubmissionThreshold = ConfigurationLocation("DIRACEnvironment", "EagerSubmissionThreshold", Some(0.2))
//  val UpdateInterval = ConfigurationLocation("DIRACEnvironment", "UpdateInterval", Some(1 minutes))
//  val JobsByGroup = ConfigurationLocation("DIRACEnvironment", "JobsByGroup", Some(10000))
//
//  def apply(
//    voName:         String,
//    service:        OptionalArgument[String]      = None,
//    group:          OptionalArgument[String]      = None,
//    bdii:           OptionalArgument[String]      = None,
//    vomsURLs:       OptionalArgument[Seq[String]] = None,
//    setup:          OptionalArgument[String]      = None,
//    fqan:           OptionalArgument[String]      = None,
//    cpuTime:        OptionalArgument[Time]        = None,
//    openMOLEMemory: OptionalArgument[Information] = None,
//    debug:          Boolean                       = false,
//    name:           OptionalArgument[String]      = None
//  )(implicit authentication: EGIAuthentication, services: BatchEnvironment.Services, cypher: Cypher, workspace: Workspace, varName: sourcecode.Name) = {
//    import services._
//
//    new DIRACEnvironment(
//      voName = voName,
//      service = service,
//      group = group,
//      bdiis = bdii.map(b ⇒ Seq(EGIEnvironment.toBDII(new URI(b)))).getOrElse(EGIEnvironment.defaultBDIIs),
//      vomsURLs = vomsURLs.getOrElse(EGIAuthentication.getVMOSOrError(voName)),
//      setup = setup.getOrElse("Dirac-Production"),
//      fqan = fqan,
//      cpuTime = cpuTime,
//      openMOLEMemory = openMOLEMemory,
//      debug = debug,
//      name = Some(name.getOrElse(varName.value))
//    )(authentication, services, cypher, workspace)
//  }
//
//}
//
//class DiracBatchExecutionJob(val job: Job, val environment: DIRACEnvironment) extends BatchExecutionJob {
//  import environment.services._
//
//  def trySelectStorage(files: ⇒ Vector[File]) = environment.trySelectAStorage(files)
//
//  def trySelectJobService() = {
//    val js = environment.jobService
//    js.tryGetToken.map(js → _)
//  }
//
//}
//
//class DIRACEnvironment(
//    val voName:                  String,
//    val service:                 Option[String],
//    val group:                   Option[String],
//    val bdiis:                   Seq[BDII],
//    val vomsURLs:                Seq[String],
//    val setup:                   String,
//    val fqan:                    Option[String],
//    val cpuTime:                 Option[Time],
//    override val openMOLEMemory: Option[Information],
//    val debug:                   Boolean,
//    override val name:           Option[String]
//)(implicit a: EGIAuthentication, val services: BatchEnvironment.Services, cypher: Cypher, workspace: Workspace) extends BatchEnvironment with BDIIStorageServers with EGIEnvironmentId { env ⇒
//
//  type JS = DIRACJobService
//
//  lazy val eagerSubmissionAgent = new EagerSubmissionAgent(WeakReference(this))
//
//  def executionJob(job: Job) = new DiracBatchExecutionJob(job, this)
//
//  @transient val authentication = DIRACAuthentication.initialise(a)(cypher)
//
//  @transient lazy val proxyCreator =
//    EGIAuthentication.initialise(a)(
//      vomsURLs,
//      voName,
//      fqan
//    )(cypher, workspace, services.preference)
//
//  @transient lazy val jobService = new DIRACJobService(env)
//
//  override def updateInterval = UpdateInterval.fixed(preference(DIRACEnvironment.UpdateInterval))
//  override def runtimeSettings = super.runtimeSettings.copy(archiveResult = true)
//
//  override def start() = {
//    super.start()
//    import services.threadProvider
//    Updater.delay(eagerSubmissionAgent)
//  }
//
//  override def stop() = {
//    super.stop()
//    eagerSubmissionAgent.stop = true
//  }
//}

