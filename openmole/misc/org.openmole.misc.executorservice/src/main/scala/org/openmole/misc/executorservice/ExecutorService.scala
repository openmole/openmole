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

package org.openmole.misc.executorservice

import java.util.concurrent.Executors
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.tools.service.DaemonThreadFactory._
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.HashMap

object ExecutorService {
  private val NbTread = new ConfigurationLocation("ExecutorService", "NbThreadsByExecutorTypes")
  Workspace += (NbTread, "20")

  private val executorServices = new HashMap[ExecutorType.Value, java.util.concurrent.ExecutorService]
  private def nbThreads = Workspace.preferenceAsInt(ExecutorService.NbTread)

  def executorService(purpose: ExecutorType.Value): java.util.concurrent.ExecutorService = {
    if (purpose == ExecutorType.OWN) return Executors.newSingleThreadExecutor(threadFactory)
    getOrCreateExecutorService(purpose)
  }
    
  private def getOrCreateExecutorService(purpose: ExecutorType.Value): java.util.concurrent.ExecutorService = {
    executorServices.getOrElseUpdate(purpose, Executors.newFixedThreadPool(nbThreads, threadFactory))
  }
}
