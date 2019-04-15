/*
 * Copyright (C) 23/09/13 Romain Reuillon
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

package org.openmole.core.authentication

import java.io.File

import org.openmole.core.serializer.SerializerService
import org.openmole.core.workspace._
import org.openmole.tool.file._

object AuthenticationStore {

  def authenticationsLocation = "authentications"

  def apply(directory: File): AuthenticationStore = {
    val dir = directory / authenticationsLocation
    if (dir.mkdirs) dir.setPosixMode("rwx------")
    new AuthenticationStore(dir)
  }

}

class AuthenticationStore(_baseDir: File) {

  def baseDir = {
    _baseDir.mkdirs
    _baseDir
  }

  def /(name: String) = new AuthenticationStore(new File(baseDir, name))

  def save(obj: Any, name: String)(implicit serializerService: SerializerService) = {
    val file = new File(baseDir, name)
    serializerService.serialize(obj, file)
  }

  def load[T](name: String)(implicit serializerService: SerializerService): T = {
    val file = new File(baseDir, name)
    loadFile(file)
  }

  def loadFile[T](file: File)(implicit serializerService: SerializerService): T =
    serializerService.deserialize[T](file)

  def delete() = {
    baseDir.recursiveDelete
    baseDir.mkdirs
  }

  def all(implicit serializerService: SerializerService) =
    baseDir.listRecursive(_.isFile).map { loadFile }

}

