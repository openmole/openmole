/**
 * Copyright (C) 2017 Jonathan Passerat-Palmbach
 * Copyright (C) 2017 Romain Reuillon
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

package org.openmole.plugin.task

import org.openmole.tool.file._

package object container {

  type FileBinding = (String, String)

  def inputPathResolver(inputDirectory: File, userWorkDirectory: String)(path: String): File = {
    if (File(path).isAbsolute) inputDirectory / path
    else inputDirectory / userWorkDirectory / path
  }

  def outputPathResolver(preparedFileBindings: Iterable[FileBinding], hostFileBindings: Iterable[FileBinding], inputDirectory: File, userWorkDirectory: String, rootDirectory: File)(filePath: String): File = {

    def isParent(dir: String, file: String) = dir.equals(File(file).getParent)
    def isPreparedFile(f: String) = preparedFileBindings.map(b ⇒ b._2).exists(b ⇒ isParent(b, f))
    def isHostFile(f: String) = hostFileBindings.map(b ⇒ b._2).exists(b ⇒ isParent(b, f))
    def isAbsolute = File(filePath).isAbsolute

    val absolutePathInCARE: String = if (isAbsolute) filePath else (File(userWorkDirectory) / filePath).getPath
    val pathToResolve = (File("/") / absolutePathInCARE).getAbsolutePath

    if (isPreparedFile(pathToResolve)) inputPathResolver(inputDirectory, userWorkDirectory)(filePath)
    else if (isHostFile(pathToResolve)) File("/") / absolutePathInCARE
    else rootDirectory / absolutePathInCARE
  }

}
