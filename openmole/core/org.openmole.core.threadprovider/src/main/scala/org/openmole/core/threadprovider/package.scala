package org.openmole.core

import scala.concurrent.ExecutionContext

package object threadprovider {

  implicit def toExecutionContext(implicit threadProvider: ThreadProvider): ExecutionContext =
    ExecutionContext.fromExecutor(threadProvider.pool)

}
