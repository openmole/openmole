package org.openmole.plugin.environment.egi

import gridscale.dirac.JobDescription
import org.openmole.core.exception.InternalProcessingError
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, BatchExecutionJob, SerializedJob }
import org.openmole.plugin.environment.batch.jobservice.JobServiceInterface
import org.openmole.plugin.environment.batch.storage.EnvironmentStorage
import org.openmole.tool.cache.TimeCache

object EGIJobService {

  implicit def isJobService: JobServiceInterface[EGIJobService] = new JobServiceInterface[EGIJobService] {
    override type J = _root_.gridscale.dirac.JobID

    override def submit(js: EGIJobService, serializedJob: SerializedJob, batchExecutionJob: BatchExecutionJob): J = js.submit(serializedJob)
    override def state(js: EGIJobService, j: J) = js.state(j)
    override def delete(js: EGIJobService, j: J): Unit = js.delete(j)
    override def stdOutErr(js: EGIJobService, j: J) = js.stdOutErr(j)
  }

  def apply(diracService: _root_.gridscale.dirac.DIRACServer, environment: EGIEnvironment[_]) =
    new EGIJobService(diracService, environment)

}

class EGIJobService(diracService: _root_.gridscale.dirac.DIRACServer, environment: EGIEnvironment[_]) {

  import environment._
  import services._
  import interpreters._

  lazy val diracJobGroup = java.util.UUID.randomUUID().toString.filter(_ != '-')

  def submit(serializedJob: SerializedJob) = accessControl {
    import org.openmole.tool.file._

    def storageLocations = storages.map(_.toOption).flatten.map(_._2).map(s ⇒ implicitly[EnvironmentStorage[WebDavStorage]].id(s) -> s.url).toMap

    def jobScript =
      JobScript(
        voName = voName,
        memory = BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt,
        threads = 1,
        debug = debug,
        storageLocations
      )

    newFile.withTmpFile("script", ".sh") { script ⇒
      script.content = jobScript(serializedJob)

      val jobDescription =
        JobDescription(
          executable = "/bin/bash",
          arguments = s"-x ${script.getName}",
          inputSandbox = Seq(script),
          stdOut = Some(EGIEnvironment.stdOutFileName),
          stdErr = Some(EGIEnvironment.stdErrFileName),
          outputSandbox = Seq(EGIEnvironment.stdOutFileName, EGIEnvironment.stdErrFileName),
          cpuTime = cpuTime
        )

      gridscale.dirac.submit(diracService, jobDescription, tokenCache(), Some(diracJobGroup))
    }
  }

  lazy val jobStateCache = TimeCache { () ⇒
    val states = gridscale.dirac.queryState(diracService, tokenCache(), groupId = Some(diracJobGroup))
    states.toMap -> preference(EGIEnvironment.JobGroupRefreshInterval)
  }

  def state(id: gridscale.dirac.JobID) = accessControl {
    val state = jobStateCache().getOrElse(id.id, throw new InternalProcessingError(s"Job ${id.id} not found in group ${diracJobGroup} of DIRAC server."))
    org.openmole.plugin.environment.gridscale.GridScaleJobService.translateStatus(state)
  }

  def delete(id: gridscale.dirac.JobID) = accessControl {
    gridscale.dirac.delete(diracService, tokenCache(), id) //clean(LocalHost(), id)
  }

  def stdOutErr(id: gridscale.dirac.JobID) = accessControl {
    newFile.withTmpDir { tmpDir ⇒
      import org.openmole.tool.file._
      tmpDir.mkdirs()
      gridscale.dirac.downloadOutputSandbox(diracService, tokenCache(), id, tmpDir)

      def stdOut =
        if ((tmpDir / EGIEnvironment.stdOutFileName) exists) (tmpDir / EGIEnvironment.stdOutFileName).content
        else ""

      def stdErr =
        if ((tmpDir / EGIEnvironment.stdErrFileName) exists) (tmpDir / EGIEnvironment.stdErrFileName).content
        else ""

      (stdOut, stdErr)
    }
  }

  lazy val accessControl = AccessControl(preference(EGIEnvironment.ConnectionsToDIRAC))
}
