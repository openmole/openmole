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
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider._
import org.openmole.core.workspace._
import org.openmole.tool.cache._
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.Seeder
import org.openmole.tool.file._
import org.openmole.core.compiler.CompilationContext

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
   * @param _newFile
   * @param fileService
   * @param workspace
   * @param _outputRedirection
   * @return
   */
  def create(
    applicationExecutionDirectory: File,
    moleExecutionDirectory:        Option[File]               = None,
    outputRedirection:             Option[OutputRedirection]  = None,
    seed:                          Option[Long]               = None,
    compilationContext:            Option[CompilationContext] = None)(implicit preference: Preference, seeder: Seeder, threadProvider: ThreadProvider, eventDispatcher: EventDispatcher, _newFile: TmpDirectory, fileService: FileService, workspace: Workspace, _outputRedirection: OutputRedirection, loggerService: LoggerService, serializerService: SerializerService, networkService: NetworkService) = {
    val executionDirectory = moleExecutionDirectory.getOrElse(applicationExecutionDirectory.newDir("execution"))

    new MoleServices(applicationExecutionDirectory, executionDirectory, compilationContext = compilationContext)(
      preference = preference,
      seeder = Seeder(seed.getOrElse(seeder.newSeed)),
      threadProvider = threadProvider,
      eventDispatcher = eventDispatcher,
      tmpDirectory = TmpDirectory(executionDirectory),
      workspace = workspace,
      fileService = fileService,
      fileServiceCache = FileServiceCache(),
      outputRedirection = outputRedirection.getOrElse(_outputRedirection),
      loggerService = loggerService,
      serializerService = serializerService,
      networkService = networkService
    )
  }

  def clean(moleServices: MoleServices) = {
    TmpDirectory.dispose(moleServices.tmpDirectory)
  }

  def copy(moleServices: MoleServices)(
    preference:        Preference        = moleServices.preference,
    seeder:            Seeder            = moleServices.seeder,
    threadProvider:    ThreadProvider    = moleServices.threadProvider,
    eventDispatcher:   EventDispatcher   = moleServices.eventDispatcher,
    newFile:           TmpDirectory      = moleServices.tmpDirectory,
    fileService:       FileService       = moleServices.fileService,
    fileServiceCache:  FileServiceCache  = moleServices.fileServiceCache,
    workspace:         Workspace         = moleServices.workspace,
    outputRedirection: OutputRedirection = moleServices.outputRedirection,
    loggerService:     LoggerService     = moleServices.loggerService,
    serializerService: SerializerService = moleServices.serializerService,
    networkService:    NetworkService    = moleServices.networkService) =
    new MoleServices(moleServices.applicationExecutionDirectory, moleServices.moleExecutionDirectory, moleServices.compilationContext)(
      preference = preference,
      seeder = seeder,
      threadProvider = threadProvider,
      eventDispatcher = eventDispatcher,
      tmpDirectory = newFile,
      workspace = workspace,
      fileService = fileService,
      fileServiceCache = fileServiceCache,
      outputRedirection = outputRedirection,
      loggerService = loggerService,
      serializerService = serializerService,
      networkService = networkService
    )
}

/**
 * implicit services for the execution of a Mole
 *
 * @param preference preferences
 * @param seeder
 * @param threadProvider
 * @param eventDispatcher
 * @param tmpDirectory
 * @param workspace
 * @param fileService
 * @param fileServiceCache
 * @param outputRedirection
 */
class MoleServices(val applicationExecutionDirectory: File, val moleExecutionDirectory: File, val compilationContext: Option[CompilationContext])(
  implicit
  val preference:        Preference,
  val seeder:            Seeder,
  val threadProvider:    ThreadProvider,
  val eventDispatcher:   EventDispatcher,
  val tmpDirectory:      TmpDirectory,
  val workspace:         Workspace,
  val fileService:       FileService,
  val fileServiceCache:  FileServiceCache,
  val outputRedirection: OutputRedirection,
  val loggerService:     LoggerService,
  val serializerService: SerializerService,
  val networkService:    NetworkService
) {
  def newRandom = Lazy(seeder.newRNG)
  implicit lazy val defaultRandom = newRandom
}
