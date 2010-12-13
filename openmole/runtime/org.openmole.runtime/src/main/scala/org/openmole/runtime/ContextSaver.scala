/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.runtime

import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.job.State._
import scala.collection.immutable.TreeMap

class ContextSaver extends IObjectListener[IMoleJob] {

  var _results = new TreeMap[MoleJobId, IContext]
  def results: TreeMap[MoleJobId, IContext] = _results

  override def eventOccured(job: IMoleJob) = synchronized {
    job.state match {
      case COMPLETED | FAILED =>
        _results += ((job.id, job.context))
      case _ =>
    }
  }
}
