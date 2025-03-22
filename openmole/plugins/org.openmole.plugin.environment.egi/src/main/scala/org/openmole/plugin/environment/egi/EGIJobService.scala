package org.openmole.plugin.environment.egi

import gridscale.dirac.JobDescription
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.EnvironmentStorage
import org.openmole.tool.cache.TimeCache

object EGIJobService:

  def apply(diracService: _root_.gridscale.dirac.DIRACServer, environment: EGIEnvironment[_])(using preferences: Preference) =
    new EGIJobService(diracService, environment, AccessControl(preferences(EGIEnvironment.ConnectionsToDIRAC)))


class EGIJobService(diracService: _root_.gridscale.dirac.DIRACServer, environment: EGIEnvironment[_], accessControl: AccessControl):

  import environment.{*, given}

  import services._

  lazy val diracJobGroup = java.util.UUID.randomUUID().toString.filter(_ != '-')

  def submit(serializedJob: SerializedJob, outputPath: String, storageLocation: String)(using AccessControl.Priority) =
    import org.openmole.tool.file.*

    newFile.withTmpFile("script", ".sh"): script =>
      script.content = JobScript.create(
        serializedJob,
        outputPath,
        storageLocation,
        voName = voName,
        memory = BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt,
        debug = debug
      )

      val jobDescription =
        JobDescription(
          executable = "/bin/bash",
          arguments = s"-x ${script.getName} -- --unique-id ${preference(Preference.uniqueID)}",
          inputSandbox = Seq(script),
          stdOut = Some(EGIEnvironment.stdOutFileName),
          stdErr = Some(EGIEnvironment.stdErrFileName),
          outputSandbox = Seq(EGIEnvironment.stdOutFileName, EGIEnvironment.stdErrFileName),
          cpuTime = cpuTime
        )

      accessControl:
        gridscale.dirac.submit(diracService, jobDescription, tokenCache(), Some(diracJobGroup))

  lazy val jobStateCache = TimeCache { () =>
    // FIXME enable again with semaphore with priority
    val states = gridscale.dirac.queryState(diracService, tokenCache(), groupId = Some(diracJobGroup))
    //val states = accessControl { gridscale.dirac.queryState(diracService, tokenCache(), groupId = Some(diracJobGroup)) }
    states.toMap -> preference(EGIEnvironment.JobGroupRefreshInterval)
  }

  def state(id: gridscale.dirac.JobID, priority: AccessControl.Priority) =
    val state = jobStateCache().getOrElse(id.id, throw new InternalProcessingError(s"Job ${id.id} not found in group ${diracJobGroup} of DIRAC server."))
    org.openmole.plugin.environment.gridscale.GridScaleJobService.translateStatus(state)

  def delete(id: gridscale.dirac.JobID, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      gridscale.dirac.delete(diracService, tokenCache(), id)

  def stdOutErr(id: gridscale.dirac.JobID, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    newFile.withTmpDir: tmpDir =>
      import org.openmole.tool.file.*
      tmpDir.mkdirs()
      accessControl { gridscale.dirac.downloadOutputSandbox(diracService, tokenCache(), id, tmpDir) }

      def stdOut =
        if ((tmpDir / EGIEnvironment.stdOutFileName) exists) (tmpDir / EGIEnvironment.stdOutFileName).content
        else ""

      def stdErr =
        if ((tmpDir / EGIEnvironment.stdErrFileName) exists) (tmpDir / EGIEnvironment.stdErrFileName).content
        else ""

      (stdOut, stdErr)


