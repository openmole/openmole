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
package org.openmole.core.script

import org.openmole.core.workspace._
import org.openmole.tool.file.FilePackage
import org.openmole.tool.random.RandomProvider
import org.openmole.tool.statistics.StatisticsPackage

trait CodePackage extends FilePackage with StatisticsPackage with MathPackage:
  export java.util.Random

  def Random(seed: Long): java.util.Random = org.openmole.tool.random.Random.apply(seed)
  def Random()(implicit randomProvider: RandomProvider): java.util.Random = newRandom(randomProvider().nextLong())
  def random(implicit randomProvider: RandomProvider) = randomProvider()

  given Conversion[java.util.Random, scala.util.Random] = scala.util.Random.javaRandomToRandom

  @deprecated("8.0")
  def newRNG(seed: Long): java.util.Random = Random(seed)
  @deprecated("8.0")
  def newRNG()(implicit randomProvider: RandomProvider): java.util.Random = Random()

  def newRandom(seed: Long): java.util.Random = Random(seed)
  def newRandom()(implicit randomProvider: RandomProvider): java.util.Random = Random()

  def newFile(prefix: String = Workspace.fixedPrefix, suffix: String = Workspace.fixedPostfix)(using TmpDirectory) = TmpDirectory.newFile(prefix, suffix)
  def newDir(prefix: String = Workspace.fixedDir)(using TmpDirectory) = TmpDirectory.newDirectory(prefix)
  def mkDir(prefix: String = Workspace.fixedDir)(using TmpDirectory) = 
    val dir = TmpDirectory.newDirectory(prefix)
    dir.mkdirs
    dir

  def classLoader[C: Manifest] = manifest[C].erasure.getClassLoader
  def classLoader(a: Any) = a.getClass.getClassLoader

  def withThreadClassLoader[R](classLoader: ClassLoader)(f: => R) =
    org.openmole.tool.thread.withThreadClassLoader(classLoader)(f)

  def abs(d: Double) = math.abs(d)
  def abs(i: Int) = math.abs(i)
  def Pi = math.Pi
  def exp(d: Double) = math.exp(d)
  def log(d: Double) = math.log(d)
  def log10(d: Double) = math.log10(d)
  def cos(d: Double) = math.cos(d)
  def sin(d: Double) = math.sin(d)
  def tan(d: Double) = math.tan(d)

  def acos(d: Double) = math.acos(d)
  def asin(d: Double) = math.asin(d)
  def atan(d: Double) = math.atan(d)

  def pow(d: Double, e: Double) = math.pow(d, e)
  def sqrt(d: Double) = math.sqrt(d)
  def round(d: Double) = math.round(d)

  def hypot(a: Double, b: Double) = math.hypot(a, b)


object CodePackage extends CodePackage {
  def namespace = s"${this.getClass.getPackage.getName}.${classOf[CodePackage].getSimpleName}"
}

trait MathPackage { mp =>

  def round(d: Double, n: Int = 0) = {
    val s = math pow (10, n)
    math.round(d * s) / s
  }

  implicit class DoubleMathDecorator(d: Double) {
    def round(n: Int = 0) = mp.round(d, n)
  }

}