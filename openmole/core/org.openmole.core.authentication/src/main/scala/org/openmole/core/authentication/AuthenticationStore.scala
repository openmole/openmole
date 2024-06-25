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

import org.openmole.core.workspace.*
import org.openmole.tool.file.*
import io.circe.*
import io.circe.syntax.*
import io.circe.parser.*

object AuthenticationStore:

  def authenticationsLocation = "authentications"

  def apply(workspace: Workspace): AuthenticationStore =
    val oldDirectory = workspace.persistentDir / authenticationsLocation
    val newDirectory = workspace.userDir / authenticationsLocation
    if oldDirectory.exists() then oldDirectory.move(newDirectory)
    if newDirectory.mkdirs then newDirectory.setPosixMode("rwx------")
    new AuthenticationStore(newDirectory)


class AuthenticationStore(_baseDir: File):

  def baseDir =
    _baseDir.mkdirs
    _baseDir

  def store(name: String) = new File(baseDir, name)

  def save[T: Encoder](name: String, obj: Seq[T]): Unit = synchronized:
    val file = new File(baseDir, name)
    file.content = obj.asJson.noSpaces

  def load[T: Decoder](name: String): Seq[T] = synchronized:
    val file = store(name)
    if file.exists()
    then decode[Seq[T]](file.content).toTry.get
    else Seq()


  def modify[T: Encoder: Decoder](name: String, m: Seq[T] => Seq[T]) = synchronized:
    save(name, m(load(name)))
  
  def clear(name: String) = synchronized:
    store(name).delete()

  def delete() = synchronized:
    baseDir.recursiveDelete
    baseDir.mkdirs


