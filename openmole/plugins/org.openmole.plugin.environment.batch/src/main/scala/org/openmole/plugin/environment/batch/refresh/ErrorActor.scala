package org.openmole.plugin.environment.batch.refresh

import java.io.FileNotFoundException

import gridscale.authentication.AuthenticationException
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.execution.Environment
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, UsageControl }
import org.openmole.tool.logger.JavaLogger

object ErrorActor extends JavaLogger {
  def receive(msg: Error)(implicit services: BatchEnvironment.Services) = {
    val Error(job, exception, output) = msg
    processError(job, exception, output)
  }

  def processError(job: BatchExecutionJob, exception: Throwable, output: Option[(String, String)])(implicit services: BatchEnvironment.Services) = {
    import Log._

    val level = exception match {
      case _: AuthenticationException     ⇒ SEVERE
      case _: UserBadDataError            ⇒ SEVERE
      case _: FileNotFoundException       ⇒ SEVERE
      case _: JobRemoteExecutionException ⇒ WARNING
      case _                              ⇒ FINE
    }

    val detailedException =
      output match {
        case None ⇒ exception
        case Some((stdOut, stdErr)) ⇒
          new InternalProcessingError(
            s"""OpenMOLE job execution failed on remote environment, stdout was:
               |$stdOut
               |stderr was:
               |$stdErr
               """.stripMargin, exception)
      }

    val er = Environment.ExceptionRaised(job, detailedException, level)
    job.environment.error(er)
    services.eventDispatcher.trigger(job.environment: Environment, er)
    logger.log(FINE, "Error in job refresh", detailedException)
  }
}
