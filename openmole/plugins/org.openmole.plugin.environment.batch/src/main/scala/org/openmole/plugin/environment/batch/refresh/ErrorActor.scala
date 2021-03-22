package org.openmole.plugin.environment.batch.refresh

import java.io.FileNotFoundException

import gridscale.authentication.AuthenticationException
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.execution.Environment
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, FailedJobExecution, AccessControl }
import org.openmole.tool.logger._

object ErrorActor {
  def receive(msg: Error)(implicit services: BatchEnvironment.Services) = {
    val Error(job, environement, exception, output) = msg
    processError(job, environement, exception, output)
  }

  def processError(job: BatchExecutionJob, environment: BatchEnvironment, exception: Throwable, output: Option[(String, String)])(implicit services: BatchEnvironment.Services) = {
    import services._

    def defaultMessage = """Failed to get the result for the job"""

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
    environment.error(er)

    services.eventDispatcher.trigger(environment: Environment, er)
    LoggerService.log(FINE, "Error in job refresh", Some(detailedException))
  }
}
