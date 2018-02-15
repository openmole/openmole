/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.core.code

import org.openmole.core.workspace._
import org.openmole.tool.file.FilePackage
import org.openmole.tool.random
import org.openmole.tool.random.RandomProvider
import org.openmole.tool.statistics.StatisticsPackage

trait CodePackage extends FilePackage with StatisticsPackage {
  def Random(seed: Long): java.util.Random = random.Random.apply(seed)
  def Random()(implicit randomProvider: RandomProvider): java.util.Random = newRandom(randomProvider().nextLong())

  @deprecated("8.0")
  def newRNG(seed: Long): java.util.Random = Random(seed)
  @deprecated("8.0")
  def newRNG()(implicit randomProvider: RandomProvider): java.util.Random = Random()

  def newRandom(seed: Long): java.util.Random = Random(seed)
  def newRandom()(implicit randomProvider: RandomProvider): java.util.Random = Random()

  def newFile(prefix: String = Workspace.fixedPrefix, suffix: String = Workspace.fixedPostfix)(implicit newFile: NewFile) = newFile.newFile(prefix, suffix)
  def newDir(prefix: String = Workspace.fixedDir)(implicit newFile: NewFile) = newFile.newDir(prefix)
  def mkDir(prefix: String = Workspace.fixedDir)(implicit newFile: NewFile) = {
    val dir = newFile.newDir(prefix)
    dir.mkdirs
    dir
  }

  def classLoader[C: Manifest] = manifest[C].erasure.getClassLoader
  def classLoader(a: Any) = a.getClass.getClassLoader

  def withThreadClassLoader[R](classLoader: ClassLoader)(f: ⇒ R) =
    org.openmole.tool.thread.withThreadClassLoader(classLoader)(f)

}

object CodePackage extends CodePackage {
  def namespace = s"${this.getClass.getPackage.getName}.${classOf[CodePackage].getSimpleName}"
}