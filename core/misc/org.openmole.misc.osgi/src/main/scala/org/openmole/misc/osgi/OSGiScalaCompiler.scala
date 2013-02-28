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

package org.openmole.misc.osgi

import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ReplGlobal
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util._
import scala.util.parsing.input.OffsetPosition
import collection.mutable.ListBuffer
import org.osgi.framework.Bundle
import java.io.{ PrintWriter, StringWriter, File }
import scala.tools.nsc.symtab.SymbolLoaders
import scala.tools.nsc.backend.JavaPlatform
import scala.tools.util.PathResolver

class OSGiScalaCompiler(settings: Settings, reporter: Reporter, virtualDirectory: AbstractFile) extends Global(settings, reporter) with ReplGlobal { g ⇒

  settings.bootclasspath.value = ClassPathBuilder.getClassPathFrom(classOf[scala.App].getClassLoader).mkString(":")

  lazy val cp = {
    val original = new PathResolver(settings).result
    val result = BundleClassPathBuilder.allBundles.map { original.context.newClassPath }.toList
    val vdcp = original.context.newClassPath(virtualDirectory)
    new MergedClassPath(result.toList ::: original :: vdcp :: Nil, original.context)
  }

  lazy val internalClassPath = {
    require(!forMSIL, "MSIL not supported")
    cp
  }

  override def classPath = internalClassPath

  /* def createClassPath[T](original: ClassPath[T]) = {
    println("create class path")
    val result = BundleClassPathBuilder.allBundles.map { original.context.newClassPath }.toList
    val vdcp = original.context.newClassPath(virtualDirectory)
    new MergedClassPath(vdcp :: result.toList ::: original :: Nil, original.context)
  }*/

  override lazy val platform = new JavaPlatform {
    val global: OSGiScalaCompiler.this.type = OSGiScalaCompiler.this
    override lazy val classPath = cp
  }

}
