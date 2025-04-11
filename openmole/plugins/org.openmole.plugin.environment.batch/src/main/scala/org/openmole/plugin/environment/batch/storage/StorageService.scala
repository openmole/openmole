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

package org.openmole.plugin.environment.batch.storage

import java.io._

import org.openmole.core.communication.storage._
import org.openmole.core.preference.PreferenceLocation
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.tool.logger.JavaLogger
import squants.time.TimeConversions._

object StorageService extends JavaLogger:

  def rmFile[S: StorageInterface](s: S, path: String, background: Boolean)(using BatchEnvironment.Services, AccessControl.Priority): Unit =
    def action =
      rmFile(s, path)
      false

    if background
    then JobManager ! RetryAction(() => action)
    else rmFile(s, path)

  def rmDirectory[S](s: S, path: String, background: Boolean)(using services: BatchEnvironment.Services, storageInterface: HierarchicalStorageInterface[S], priority: AccessControl.Priority): Unit =
    def action =
      rmDirectory(s, path)
      false

    if background
    then JobManager ! RetryAction(() => action)
    else rmDirectory(s, path)

  def rmFile[S](s: S, directory: String)(using storageInterface: StorageInterface[S], priority: AccessControl.Priority): Unit =
    storageInterface.rmFile(s, directory)

  def rmDirectory[S](s: S, directory: String)(using hierarchicalStorageInterface: HierarchicalStorageInterface[S], priority: AccessControl.Priority): Unit =
    hierarchicalStorageInterface.rmDir(s, directory)

  def id[S](s: S)(implicit environmentStorage: EnvironmentStorage[S]) = environmentStorage.id(s)

  def download[S](s: S, src: String, dest: File, options: TransferOptions = TransferOptions.default)(using storageService: StorageInterface[S], priority: AccessControl.Priority) =
    storageService.download(s, src, dest, options)

  def upload[S](s: S, src: File, dest: String, options: TransferOptions = TransferOptions.default)(using storageInterface: StorageInterface[S], priority: AccessControl.Priority) =
    storageInterface.upload(s, src, dest, options)

  def child[S](s: S, path: String, name: String)(using storageService: HierarchicalStorageInterface[S], priority: AccessControl.Priority) = storageService.child(s, path, name)

  def exists[S](s: S, path: String)(using storageInterface: StorageInterface[S], priority: AccessControl.Priority) =
    storageInterface.exists(s, path)

  def uploadInDirectory[S: {StorageInterface, HierarchicalStorageInterface}](s: S, file: File, directory: String, transferOptions: TransferOptions)(using AccessControl.Priority) =
    val path = child(s, directory, StorageSpace.timedUniqName)
    upload(s, file, path, transferOptions)
    path



