package org.openmole.plugin.environment.batch.refresh

import java.io.FileNotFoundException

import gridscale.authentication.AuthenticationException
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.execution.Environment
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, FailedJobExecution, AccessControl }
import org.openmole.tool.logger._

object ErrorActor {
  def receive(msg: Error)(implicit services: BatchEnvironment.Services) = {
    val Error(job, environment, exception, output, openMOLEOutput) = msg
    processError(job, environment, exception, output, openMOLEOutput)
  }

  def processError(job: BatchExecutionJob, environment: BatchEnvironment, exception: Throwable, output: Option[(String, String)], openMOLEOutput: Option[String])(implicit services: BatchEnvironment.Services) = {
    import services._

    def defaultMessage = """Failed to get the result for the job"""

    val (level, message, cause) = exception match {
      case _: AuthenticationException => (SEVERE, defaultMessage, exception)
      case _: UserBadDataError => (SEVERE, defaultMessage, exception)
      case _: FileNotFoundException => (SEVERE, defaultMessage, exception)
      case e: GetResultActor.JobRemoteExecutionException => (WARNING, e.message, e.cause)
      case _ => (FINE, defaultMessage, exception)
    }

    val detailedException =
      output match {
        case None => exception
        case Some((stdOut, stdErr)) =>

          def openMOLEOutputMessage =
            openMOLEOutput match {
              case Some(o) =>
                s"""OpenMOLE output was:
                   |$o
                   |""".stripMargin
              case None => ""
            }

          FailedJobExecution(
            message = message,
            cause = cause,
            detail = openMOLEOutputMessage +
              s"""Stdout was:
               |$stdOut
               |stderr was:
               |$stdErr
               """.stripMargin
          )
      }

    def details =
      (output, openMOLEOutput) match {
        case (None, None) => None
        case _ =>
          def openMOLEOutputMessage =
            openMOLEOutput map { o =>
              s"""OpenMOLE output was:
                 |$o""".stripMargin
            }

          def outputMessage =
            output map {
              case (stdOut, stdErr) =>
                s"""Stdout of the job was:
                |$stdOut
                |Stderr of the job was:
                |$stdErr """.stripMargin
            }

          Some(Seq(openMOLEOutputMessage, outputMessage).flatten.mkString("\n"))
      }

    val er = Environment.ExecutionJobExceptionRaised(job, detailedException, level, details)
    environment.error(er)

    services.eventDispatcher.trigger(environment: Environment, er)
    LoggerService.log(FINE, "Error in job refresh", Some(detailedException))
  }
}
