package org.openmole.plugin.environment.batch

import org.openmole.core.exception.InternalProcessingError

package object environment:
  case class FailedJobExecution(message: String, cause: Throwable, detail: String) extends InternalProcessingError(message, cause)

