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

object DeleteActor extends Logger

import DeleteActor._

class DeleteActor extends Actor {
  def receive = {
    case DeleteFile(storage, path, directory) ⇒
      try
        storage.withToken { implicit t ⇒
          if (directory) storage.rmDir(path) else storage.rmFile(path)
        }
      catch {
        case t: Throwable ⇒
          logger.log(FINE, "Error when deleting a file", t)
      }
      System.runFinalization
  }
}