/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.fileservice

import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.tool.thread._
import org.openmole.tool.file._

import scala.ref.{ ReferenceQueue, WeakReference }

private class AsynchronousDeleter(fileService: WeakReference[FileService]):
  def stop = !fileService.get.isDefined

  private val cleanFiles = new LinkedBlockingQueue[File]
  def asynchronousRemove(file: File) = cleanFiles.add(file)

  private def run =
    while !cleanFiles.isEmpty || !stop
    do cleanFiles.take.recursiveDelete

  def start(implicit threadProvider: ThreadProvider) = threadProvider.virtual(() => run)


