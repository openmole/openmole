/*
 * Copyright (C) 22/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.mole

import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.{ FileService, FileServiceCache }
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider._
import org.openmole.core.workspace._
import org.openmole.tool.cache._
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.Seeder

object MoleExecutionContext {
  def apply()(implicit moleServices: MoleServices) = new MoleExecutionContext()
}

/**
 * Wrapper for [[MoleServices]] which are implicit
 * @param services implicit services
 */
class MoleExecutionContext(implicit val services: MoleServices)

object MoleServices {

  /**
   * create a MoleService from implicit parameters
   * @param preference
   * @param seeder
   * @param threadProvider
   * @param eventDispatcher
   * @param newFile
   * @param fileService
   * @param workspace
   * @param outputRedirection
   * @return
   */
  implicit def create(implicit preference: Preference, seeder: Seeder, threadProvider: ThreadProvider, eventDispatcher: EventDispatcher, newFile: NewFile, fileService: FileService, workspace: Workspace, outputRedirection: OutputRedirection, loggerService: LoggerService) = {
    new MoleServices()(
      preference = preference,
      seeder = Seeder(seeder.newSeed),
      threadProvider = threadProvider,
      eventDispatcher = eventDispatcher,
      newFile = NewFile(newFile.newDir("execution")),
      workspace = workspace,
      fileService = fileService,
      fileServiceCache = FileServiceCache(),
      outputRedirection = outputRedirection,
      loggerService = loggerService
    )
  }

  def copy(moleServices: MoleServices)(
    preference:        Preference        = moleServices.preference,
    seeder:            Seeder            = moleServices.seeder,
    threadProvider:    ThreadProvider    = moleServices.threadProvider,
    eventDispatcher:   EventDispatcher   = moleServices.eventDispatcher,
    newFile:           NewFile           = moleServices.newFile,
    fileService:       FileService       = moleServices.fileService,
    fileServiceCache:  FileServiceCache  = moleServices.fileServiceCache,
    workspace:         Workspace         = moleServices.workspace,
    outputRedirection: OutputRedirection = moleServices.outputRedirection,
    loggerService:     LoggerService     = moleServices.loggerService) =
    new MoleServices()(
      preference = preference,
      seeder = seeder,
      threadProvider = threadProvider,
      eventDispatcher = eventDispatcher,
      newFile = newFile,
      workspace = workspace,
      fileService = fileService,
      fileServiceCache = fileServiceCache,
      outputRedirection = outputRedirection,
      loggerService = loggerService
    )
}

/**
 * implicit services for the execution of a Mole
 *
 * @param preference preferences
 * @param seeder
 * @param threadProvider
 * @param eventDispatcher
 * @param newFile
 * @param workspace
 * @param fileService
 * @param fileServiceCache
 * @param outputRedirection
 */
class MoleServices(
  implicit
  val preference:        Preference,
  val seeder:            Seeder,
  val threadProvider:    ThreadProvider,
  val eventDispatcher:   EventDispatcher,
  val newFile:           NewFile,
  val workspace:         Workspace,
  val fileService:       FileService,
  val fileServiceCache:  FileServiceCache,
  val outputRedirection: OutputRedirection,
  val loggerService:     LoggerService
) {
  def newRandom = Lazy(seeder.newRNG)
  implicit lazy val defaultRandom = newRandom
}
