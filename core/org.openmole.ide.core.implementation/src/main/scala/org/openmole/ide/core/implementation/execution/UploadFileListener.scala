/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.execution

import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.misc.eventdispatcher._

class UploadFileListener(exeManager: ExecutionManager) extends EventListener[BatchEnvironment] {
  override def triggered(environment: BatchEnvironment, event: Event[BatchEnvironment]) = {
    event match {
      case x: BeginUpload ⇒ exeManager.uploads = (exeManager.uploads._1, exeManager.uploads._2 + 1)
      case x: EndUpload ⇒ exeManager.uploads = (exeManager.uploads._1 + 1, exeManager.uploads._2)
      case x: BeginDownload ⇒ exeManager.downloads = (exeManager.downloads._1, exeManager.downloads._2 + 1)
      case x: EndDownload ⇒ exeManager.downloads = (exeManager.downloads._1 + 1, exeManager.downloads._2)
    }
    exeManager.displayFileTransfer
  }
}