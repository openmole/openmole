/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.updater.internal

import java.util.concurrent.Executors
import org.openmole.commons.tools.service.DaemonThreadFactory._
import Activator._
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.updater.IUpdatable
import org.openmole.misc.updater.IUpdater
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import java.util.concurrent.TimeUnit

object Updater extends IUpdater {
  
    var shutDown = false
    lazy val scheduler =  Executors.newScheduledThreadPool(1,threadFactory)

    override def registerForUpdate(updatable: IUpdatableWithVariableDelay, purpose: ExecutorType.Value) = {
        val task = new UpdaterTask(updatable, purpose)
        executorService.executorService(purpose).submit(task)
    }

    override def delay(updatable: IUpdatableWithVariableDelay, purpose: ExecutorType.Value) = {
        val task = new UpdaterTask(updatable, purpose)
        delay(task)
    }

    override def registerForUpdate(updatable: IUpdatable, purpose: ExecutorType.Value, updateInterval: Long) = {
        registerForUpdate(new UpdatableWithFixedDelay(updatable, updateInterval), purpose)
    }

    override def delay(updatable: IUpdatable, purpose: ExecutorType.Value, updateInterval: Long) = {
        delay(new UpdatableWithFixedDelay(updatable, updateInterval), purpose)
    }

    def delay(updaterTask: UpdaterTask) = {
        if (!shutDown) {

            scheduler.schedule(new Runnable {
                override def run = {
                  executorService.executorService(updaterTask.purpose).submit(updaterTask);
                }
              }, updaterTask.updatable.delay, TimeUnit.MILLISECONDS)

        }

    }

}
