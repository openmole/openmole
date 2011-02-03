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

package org.openmole.misc.executorservice.internal

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.executorservice.IExecutorService
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.commons.tools.service.DaemonThreadFactory._
import scala.collection.mutable.HashMap
import Activator._

object ExecutorService {
  val NbTread = new ConfigurationLocation("ExecutorService", "NbThreadsByExecutorTypes")
  workspace += (NbTread, "20")
}

class ExecutorService extends IExecutorService {

  val executorServices = new HashMap[ExecutorType.Value, java.util.concurrent.ExecutorService]
  def nbThreads = workspace.preferenceAsInt(ExecutorService.NbTread)


  override def executorService(purpose: ExecutorType.Value): java.util.concurrent.ExecutorService = {
    if (purpose == ExecutorType.OWN) return Executors.newSingleThreadExecutor(threadFactory)
    getOrCreateExecutorService(purpose)
  }
    
  def getOrCreateExecutorService(purpose: ExecutorType.Value): java.util.concurrent.ExecutorService = {
    executorServices.getOrElseUpdate(purpose, Executors.newFixedThreadPool(nbThreads, threadFactory))
  }
}
