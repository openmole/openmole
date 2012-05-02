/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.batch.refresh

import akka.actor.Actor
import org.openmole.misc.tools.service.Logger

object KillerActor extends Logger

import KillerActor._

class KillerActor extends Actor {
  def receive = {
    case KillBatchJob(bj) ⇒
      try bj.kill
      catch {
        case e ⇒ logger.log(FINE, "Could not kill job.", e)
      }
      System.runFinalization
  }
}
