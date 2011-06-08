/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.updater.internal

import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.updater.IUpdatableWithVariableDelay

class UpdaterTask(val updatable: IUpdatableWithVariableDelay, val purpose: ExecutorType.Value) extends Runnable {

  override def run = {
    try {
      val resubmit = updatable.update
      System.runFinalization
      if(resubmit) Updater.delay(this)
    } catch {
      case e => Logger.getLogger(classOf[UpdaterTask].getName).log(Level.WARNING, null, e)
    }
  }
}
