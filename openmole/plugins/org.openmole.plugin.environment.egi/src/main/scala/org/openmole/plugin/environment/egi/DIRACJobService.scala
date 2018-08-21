///*
// * Copyright (C) 10/06/13 Romain Reuillon
// * Copyright (C) 2014 Jonathan Passerat-Palmbach
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
//import fr.iscpif.gridscale.egi._
//import fr.iscpif.gridscale.{ egi ⇒ gridscale }
//import org.openmole.tool.file._
//import org.openmole.plugin.environment.batch.control.UnlimitedAccess
//import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, SerializedJob }
//import org.openmole.plugin.environment.gridscale.GridScaleJobService
//import org.openmole.tool.file.uniqName
//import org.openmole.tool.logger.Logger
//import squants.time.TimeConversions._
//
//object DIRACJobService extends Logger
//
//import org.openmole.plugin.environment.egi.DIRACJobService._
//
//class DIRACJobService(val environment: DIRACEnvironment) extends GridScaleJobService { js ⇒
//
//  def getToken = jobService.jobService.token
//
//  import environment.services._
//
//  def accessControl = UnlimitedAccess
//
//  def diracService = {
//    lazy val gridscale.DIRACJobService.Service(service, group) = gridscale.DIRACJobService.getService(environment.voName)
//
//    val serviceValue = environment.service.getOrElse(service)
//    val groupValue = environment.group.getOrElse(group)
//
//    gridscale.DIRACJobService.Service(serviceValue, groupValue)
//  }
//
//  val jobService = {
//    import environment.services._
//    val js =
//      gridscale.DIRACGroupedJobService(
//        environment.voName,
//        service = Some(diracService),
//        statusQueryInterval = environment.updateInterval.minUpdateInterval,
//        jobsByGroup = preference(DIRACEnvironment.JobsByGroup)
//      )(environment.authentication)
//
//    js.delegate(environment.authentication.certificate, environment.authentication.password)
//    js
//  }
//
//  def jobScript =
//    JobScript(
//      voName = environment.voName,
//      memory = BatchEnvironment.openMOLEMemoryValue(environment.openMOLEMemory).toMegabytes.toInt,
//      threads = BatchEnvironment.threadsValue(environment.threads),
//      debug = environment.debug
//    )
//
//  def id = diracService
//
//  protected def _submit(serializedJob: SerializedJob) = {
//    import environment.services._
//    import serializedJob._
//
//    val script = newFile.newFile("script", ".sh")
//    try {
//      val outputFilePath = storage.child(path, uniqName("job", ".out"))
//
//      script.content = jobScript(serializedJob, outputFilePath, None, None)
//
//      val jobDescription = gridscale.DIRACJobDescription(
//        stdOut = if (environment.debug) Some("out") else None,
//        stdErr = if (environment.debug) Some("err") else None,
//        outputSandbox = if (environment.debug) Seq("out" → newFile.newFile("job", ".out"), "err" → newFile.newFile("job", ".err")) else Seq.empty,
//        inputSandbox = Seq(script),
//        arguments = script.getName,
//        executable = "/bin/bash",
//        cpuTime = environment.cpuTime.map(x ⇒ x: concurrent.duration.Duration)
//      )
//
//      val jid = jobService.submit(jobDescription)
//      Log.logger.fine(s"""DIRAC job [${jid}], description: \n${jobDescription}""")
//
//      new DIRACJob {
//        val jobService = js
//        val storage = serializedJob.storage
//        def resultPath = outputFilePath
//        def id = jid
//      }
//    }
//    finally script.delete
//  }
//
//  override protected def _delete(j: J) = if (!environment.debug) super._delete(j)
//}
