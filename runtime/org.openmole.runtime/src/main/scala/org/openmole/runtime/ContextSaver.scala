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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.runtime

import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.ITimeStamp
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.job.State._
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.misc.tools.service.Logger
import scala.collection.immutable.TreeMap

object ContextSaver extends Logger

class ContextSaver extends IObjectListenerWithArgs[IMoleJob] {

  import ContextSaver._
  
  var _results = new TreeMap[MoleJobId, (Either[IContext,Throwable], Seq[ITimeStamp])]
  def results = _results

  override def eventOccured(job: IMoleJob, args: Array[Object]) = synchronized {
    val state = args(0).asInstanceOf[State]
    state match {
      case COMPLETED | FAILED =>
        job.exception match {
          case None => _results += job.id -> (Left(job.context), job.timeStamps)
          case Some(t) => _results += job.id -> (Right(t), job.timeStamps)
        }
      case _ =>
    }
  }
  
}
