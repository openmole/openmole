package org.openmole.web

import org.openmole.misc.eventdispatcher.{ Event, EventListener }
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.IMoleExecution.{ Finished, Starting, JobCreated, JobStatusChanged }
import org.openmole.misc.tools.io.TarArchiver.TarOutputStream2TarOutputStreamComplement
import com.ice.tar.TarOutputStream
import java.io.ByteArrayOutputStream

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/14/13
 * Time: 1:34 PM
 */
class JobEventListener(d: DataHandler[String, Stats.Stats], cacheMap: DataHandler[IMoleExecution, String]) extends EventListener[IMoleExecution] {

  def updateMap(m: Stats.Stats, key: String, value: Int ⇒ Int) = m + (key -> value(m get key getOrElse 0))

  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = {
    val moleId = cacheMap.get(execution).get
    event match {
      case x: JobCreated       ⇒ d.add(moleId, updateMap(d get moleId getOrElse Stats.empty, "Ready", _ + 1))
      case x: JobStatusChanged ⇒ d.add(moleId, updateMap(updateMap(d get moleId getOrElse Stats.empty, x.newState.name, _ + 1), x.oldState.name, _ - 1))
    }
  }
}

class MoleStatusListener(mH: MoleHandling) extends EventListener[IMoleExecution] {
  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = {
    event match {
      case x: Starting ⇒ mH.setStatus(execution, MoleHandling.Status.running)
      case x: Finished ⇒ {
        mH.setStatus(execution, MoleHandling.Status.finished)
        mH.decacheMole(execution)
        mH.storeResultBlob(execution)
      }
    }
  }
}

import java.io.OutputStream

class webStream extends OutputStream {
  def write(p1: Int) {
    storage += p1.toChar
  }

  var storage = ""
}
