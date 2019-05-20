package org.openmole.plugin.environment.batch.refresh

import java.io.FileNotFoundException

import gridscale.authentication.AuthenticationException
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.execution.Environment
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, FailedJobExecution, AccessControl }
import org.openmole.tool.logger._

object ErrorActor {
  def receive(msg: Error)(implicit services: BatchEnvironment.Services) = {
    val Error(job, exception, output) = msg
    processError(job, exception, output)
  }

  def processError(job: BatchExecutionJob, exception: Throwable, output: Option[(String, String)])(implicit services: BatchEnvironment.Services) = {
    import services._

    def defaultMessage = """OpenMOLE job execution failed on remote environment"""

    val (level, message, cause) = exception match {
      case _: AuthenticationException ⇒ (SEVERE, defaultMessage, exception)
      case _: UserBadDataError ⇒ (SEVERE, defaultMessage, exception)
      case _: FileNotFoundException ⇒ (SEVERE, defaultMessage, exception)
      case e: GetResultActor.JobRemoteExecutionException ⇒ (WARNING, e.message, e.cause)
      case _ ⇒ (FINE, defaultMessage, exception)
    }

    val detailedException =
      output match {
        case None ⇒ exception
        case Some((stdOut, stdErr)) ⇒
          new FailedJobExecution(
            message = message,
            cause = cause,
            detail =
              s"""Stdout was:
               |$stdOut
               |stderr was:
               |$stdErr
               """.stripMargin
          )
      }

    val er = Environment.ExecutionJobExceptionRaised(job, detailedException, level)
    job.environment.error(er)

    services.eventDispatcher.trigger(job.environment: Environment, er)
    LoggerService.log(FINE, "Error in job refresh", Some(detailedException))
  }
}
