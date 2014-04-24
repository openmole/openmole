package org.openmole.web.mole

import org.openmole.misc.eventdispatcher.{ Event, EventListener }
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.IMoleExecution.{ Finished, Starting, JobCreated, JobStatusChanged }
import org.openmole.web.db.tables.MoleStats
import org.openmole.web.cache.{ Status, DataHandler }
import org.openmole.core.model.job

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/14/13
 * Time: 1:34 PM
 */
class JobEventListener(mH: MoleHandling) extends EventListener[IMoleExecution] {

  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = {
    val stats = mH.getMoleStats(execution)

    implicit def state2Lens(s: job.State.State) = {
      import job.State._
      s match {
        case READY     ⇒ stats.lens.ready
        case RUNNING   ⇒ stats.lens.running
        case COMPLETED ⇒ stats.lens.completed
        case CANCELED  ⇒ stats.lens.cancelled
        case FAILED    ⇒ stats.lens.failed
        case _         ⇒ throw new Exception(s"Unknown job status: $s")
      }
    }

    event match {
      case x: JobCreated       ⇒ mH.updateStats(execution, stats.lens.ready++)
      case x: JobStatusChanged ⇒ { mH.updateStats(execution, x.newState++); mH.updateStats(execution, x.oldState--) }
    }
  }
}

class MoleStatusListener(mH: MoleHandling) extends EventListener[IMoleExecution] {
  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = {
    event match {
      case x: Starting ⇒ mH.setStatus(execution, Status.Running)
      case x: Finished ⇒ {
        mH.setStatus(execution, Status.Finished)
        mH.storeResultBlob(execution)
        mH.decacheMole(execution)
      }
    }
  }
}