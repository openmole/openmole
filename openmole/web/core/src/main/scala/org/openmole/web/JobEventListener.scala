package org.openmole.web

import org.openmole.misc.eventdispatcher.{ Event, EventListener }
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.IMoleExecution.JobCreated
import org.openmole.core.model.job.State._
import org.openmole.core.model.mole.IMoleExecution.JobCreated
import org.openmole.core.model.mole.IMoleExecution.JobStatusChanged

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/14/13
 * Time: 1:34 PM
 */
class JobEventListener(d: DataHandler[String, Stats.Stats]) extends EventListener[IMoleExecution] {

  def updateMap(m: Stats.Stats, key: String, value: Int ⇒ Int) = m + (key -> value(m get key getOrElse 0))

  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = {
    event match {
      case x: JobCreated       ⇒ d.add(execution.id, updateMap(d get execution.id getOrElse Stats.empty, "Ready", _ + 1))
      case x: JobStatusChanged ⇒ d.add(execution.id, updateMap(updateMap(d get execution.id getOrElse Stats.empty, x.newState.name, _ + 1), x.oldState.name, _ - 1))
    }
  }
}
